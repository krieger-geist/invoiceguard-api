package com.invoiceguard.invoice.service;

import com.invoiceguard.exception.BusinessRuleViolationException;
import com.invoiceguard.exception.InvalidStateTransitionException;
import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.invoice.dto.InvoiceCreateRequest;
import com.invoiceguard.invoice.dto.InvoiceItemRequest;
import com.invoiceguard.invoice.dto.InvoiceUpdateRequest;
import com.invoiceguard.invoice.entity.IdempotencyRecord;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceItem;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import com.invoiceguard.invoice.entity.InvoiceStatusHistory;
import com.invoiceguard.invoice.repository.InvoiceItemRepository;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import com.invoiceguard.invoice.repository.InvoiceStatusHistoryRepository;
import com.invoiceguard.invoice.util.InvoiceNumberNormalizer;
import com.invoiceguard.security.AuthenticatedPrincipal;
import com.invoiceguard.security.Role;
import com.invoiceguard.security.TenantContext;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.service.VendorService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.invoiceguard.invoice.specification.InvoiceSpecifications.amountBetween;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.belongsToOrganization;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.createdBy;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.dueDateBetween;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.hasPurchaseOrderNumber;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.hasRiskLevel;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.hasStatus;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.hasVendor;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.invoiceDateBetween;
import static com.invoiceguard.invoice.specification.InvoiceSpecifications.keyword;

@Service
public class InvoiceService {

    /** Cents-level tolerance for subtotal + tax - discount = total, to absorb rounding noise. */
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    /** Never editable, regardless of role — the invoice is mid-processing. */
    private static final Set<InvoiceStatus> HARD_LOCKED_STATUSES = EnumSet.of(InvoiceStatus.ANALYSING);

    /** Editable only by an organisation admin (or higher) — see {@code canOverrideLock}. */
    private static final Set<InvoiceStatus> PERMISSION_LOCKED_STATUSES = EnumSet.of(
            InvoiceStatus.APPROVED, InvoiceStatus.REJECTED, InvoiceStatus.PAID, InvoiceStatus.ARCHIVED,
            InvoiceStatus.CANCELLED);

    private static final Map<InvoiceStatus, Set<InvoiceStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(InvoiceStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(InvoiceStatus.DRAFT, EnumSet.of(InvoiceStatus.SUBMITTED, InvoiceStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(InvoiceStatus.SUBMITTED, EnumSet.of(InvoiceStatus.ANALYSING, InvoiceStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(
                InvoiceStatus.ANALYSING, EnumSet.of(InvoiceStatus.REVIEW_REQUIRED, InvoiceStatus.APPROVED));
        ALLOWED_TRANSITIONS.put(
                InvoiceStatus.REVIEW_REQUIRED, EnumSet.of(InvoiceStatus.APPROVED, InvoiceStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(InvoiceStatus.APPROVED, EnumSet.of(InvoiceStatus.PAID, InvoiceStatus.ARCHIVED));
        ALLOWED_TRANSITIONS.put(InvoiceStatus.REJECTED, EnumSet.of(InvoiceStatus.ARCHIVED));
        ALLOWED_TRANSITIONS.put(InvoiceStatus.PAID, EnumSet.of(InvoiceStatus.ARCHIVED));
    }

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceStatusHistoryRepository statusHistoryRepository;
    private final VendorService vendorService;
    private final IdempotencyService idempotencyService;
    private final TenantContext tenantContext;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceItemRepository invoiceItemRepository,
            InvoiceStatusHistoryRepository statusHistoryRepository,
            VendorService vendorService,
            IdempotencyService idempotencyService,
            TenantContext tenantContext,
            org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.vendorService = vendorService;
        this.idempotencyService = idempotencyService;
        this.tenantContext = tenantContext;
        this.eventPublisher = eventPublisher;
    }

    // ---- Create, with idempotency ------------------------------------------------------

    @Transactional
    public InvoiceCreationOutcome create(InvoiceCreateRequest request, String idempotencyKey) {
        UUID organizationId = tenantContext.requireOrganizationId();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String requestHash = idempotencyService.hashRequest(request);
            Optional<IdempotencyRecord> existing = idempotencyService.find(organizationId, idempotencyKey);
            if (existing.isPresent()) {
                idempotencyService.assertMatches(existing.get(), requestHash);
                Invoice invoice = getOwnedById(existing.get().getResponseReference());
                // The original request created the resource (201). A replay
                // creates nothing, so it returns the saved resource as 200.
                return new InvoiceCreationOutcome(invoice, 200, true);
            }

            Invoice created = createInternal(request, organizationId, idempotencyKey);
            idempotencyService.save(organizationId, idempotencyKey, requestHash, created.getId(), 201);
            return new InvoiceCreationOutcome(created, 201, false);
        }

        Invoice created = createInternal(request, organizationId, null);
        return new InvoiceCreationOutcome(created, 201, false);
    }

    private Invoice createInternal(InvoiceCreateRequest request, UUID organizationId, String idempotencyKey) {
        Vendor vendor = vendorService.getOwnedById(request.vendorId());

        BigDecimal discount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        validateTotals(request.subtotal(), request.taxAmount(), discount, request.totalAmount());

        Invoice invoice = new Invoice();
        invoice.setOrganizationId(organizationId);
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setNormalizedInvoiceNumber(InvoiceNumberNormalizer.normalize(request.invoiceNumber()));
        invoice.setVendorId(vendor.getId());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setCurrency(request.currency());
        invoice.setSubtotal(scale(request.subtotal()));
        invoice.setTaxAmount(scale(request.taxAmount()));
        invoice.setDiscountAmount(scale(discount));
        invoice.setTotalAmount(scale(request.totalAmount()));
        invoice.setPurchaseOrderNumber(request.purchaseOrderNumber());
        invoice.setPaymentReference(request.paymentReference());
        invoice.setDescription(request.description());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIdempotencyKey(idempotencyKey);

        invoice = invoiceRepository.save(invoice);
        saveItems(invoice, request.items());
        recordHistory(invoice, null, InvoiceStatus.DRAFT, "Invoice created");

        return invoice;
    }

    private void saveItems(Invoice invoice, List<InvoiceItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (InvoiceItemRequest itemRequest : items) {
            InvoiceItem item = new InvoiceItem();
            item.setOrganizationId(invoice.getOrganizationId());
            item.setInvoiceId(invoice.getId());
            item.setDescription(itemRequest.description());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.unitPrice());
            BigDecimal taxRate = itemRequest.taxRate() != null ? itemRequest.taxRate() : BigDecimal.ZERO;
            item.setTaxRate(taxRate);

            BigDecimal lineSubtotal = itemRequest.quantity().multiply(itemRequest.unitPrice());
            BigDecimal lineTax = lineSubtotal.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineSubtotal.add(lineTax).setScale(2, RoundingMode.HALF_UP);
            item.setTaxAmount(lineTax);
            item.setLineTotal(lineTotal);
            item.setCategory(itemRequest.category());
            item.setProductCode(itemRequest.productCode());

            invoiceItemRepository.save(item);
            itemsTotal = itemsTotal.add(lineTotal);
        }

        BigDecimal expected = invoice.getSubtotal().add(invoice.getTaxAmount());
        if (itemsTotal.subtract(expected).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessRuleViolationException(
                    "Sum of line items (%s) does not match subtotal + tax (%s)".formatted(itemsTotal, expected));
        }
    }

    private void validateTotals(BigDecimal subtotal, BigDecimal tax, BigDecimal discount, BigDecimal total) {
        BigDecimal expected = subtotal.add(tax).subtract(discount);
        if (expected.subtract(total).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new BusinessRuleViolationException(
                    "totalAmount (%s) does not equal subtotal + tax - discount (%s)".formatted(total, expected));
        }
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // ---- Read / search ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Invoice getOwnedById(UUID invoiceId) {
        UUID organizationId = tenantContext.requireOrganizationId();
        return invoiceRepository
                .findByIdAndOrganizationId(invoiceId, organizationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Invoice", invoiceId));
    }

    @Transactional(readOnly = true)
    public List<InvoiceItem> getItems(UUID invoiceId) {
        return invoiceItemRepository.findByInvoiceIdOrderByCreatedAtAsc(invoiceId);
    }

    /**
     * Writes the risk engine's output onto the invoice. Deliberately bypasses
     * {@link #assertEditable} — this is the engine updating its own derived
     * fields (risk level/score), not a user editing invoice content, and it
     * runs precisely while the invoice is in the {@code ANALYSING} status
     * that {@code assertEditable} otherwise locks against user edits.
     */
    @Transactional
    public Invoice applyRiskResult(UUID invoiceId, com.invoiceguard.invoice.entity.InvoiceRiskLevel riskLevel, int score) {
        Invoice invoice = getOwnedById(invoiceId);
        invoice.setRiskLevel(riskLevel);
        invoice.setLatestRiskScore(score);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceStatusHistory> getHistory(UUID invoiceId) {
        getOwnedById(invoiceId); // tenant + existence check
        return statusHistoryRepository.findByInvoiceIdOrderByCreatedAtAsc(invoiceId);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> search(InvoiceSearchFilter filter, Pageable pageable) {
        UUID organizationId = tenantContext.requireOrganizationId();
        Specification<Invoice> spec = Specification.where(belongsToOrganization(organizationId))
                .and(hasVendor(filter.vendorId()))
                .and(hasStatus(filter.status()))
                .and(hasRiskLevel(filter.riskLevel()))
                .and(hasPurchaseOrderNumber(filter.purchaseOrderNumber()))
                .and(createdBy(filter.createdBy()))
                .and(amountBetween(filter.minAmount(), filter.maxAmount()))
                .and(invoiceDateBetween(filter.invoiceDateFrom(), filter.invoiceDateTo()))
                .and(dueDateBetween(filter.dueDateFrom(), filter.dueDateTo()))
                .and(keyword(filter.keyword()));
        return invoiceRepository.findAll(spec, pageable);
    }

    // ---- Update -----------------------------------------------------------------------

    @Transactional
    public Invoice update(UUID invoiceId, InvoiceUpdateRequest request) {
        Invoice invoice = getOwnedById(invoiceId);
        assertEditable(invoice);

        BigDecimal discount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        validateTotals(request.subtotal(), request.taxAmount(), discount, request.totalAmount());

        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDueDate(request.dueDate());
        invoice.setCurrency(request.currency());
        invoice.setSubtotal(scale(request.subtotal()));
        invoice.setTaxAmount(scale(request.taxAmount()));
        invoice.setDiscountAmount(scale(discount));
        invoice.setTotalAmount(scale(request.totalAmount()));
        invoice.setPurchaseOrderNumber(request.purchaseOrderNumber());
        invoice.setPaymentReference(request.paymentReference());
        invoice.setDescription(request.description());

        invoiceItemRepository.deleteByInvoiceId(invoice.getId());
        saveItems(invoice, request.items());

        return invoiceRepository.save(invoice);
    }

    @Transactional
    public void softDelete(UUID invoiceId) {
        Invoice invoice = getOwnedById(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT invoices can be deleted");
        }
        invoice.setDeleted(true);
        invoiceRepository.save(invoice);
    }

    private void assertEditable(Invoice invoice) {
        if (HARD_LOCKED_STATUSES.contains(invoice.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Invoice cannot be modified while status is " + invoice.getStatus());
        }
        if (PERMISSION_LOCKED_STATUSES.contains(invoice.getStatus()) && !callerCanOverrideLock()) {
            throw new BusinessRuleViolationException(
                    "Invoice status " + invoice.getStatus() + " can only be modified by an organisation admin");
        }
    }

    private boolean callerCanOverrideLock() {
        return tenantContext
                .currentPrincipal()
                .map(AuthenticatedPrincipal::role)
                .map(role -> role == Role.ORGANIZATION_ADMIN || role == Role.SUPER_ADMIN)
                .orElse(false);
    }

    // ---- Status transitions -------------------------------------------------------------

    @Transactional
    public Invoice submit(UUID invoiceId) {
        Invoice invoice = transition(invoiceId, InvoiceStatus.SUBMITTED, "Submitted for analysis");
        eventPublisher.publishEvent(
                new com.invoiceguard.invoice.event.InvoiceSubmittedEvent(invoice.getId(), invoice.getOrganizationId(), tenantContext.requireUserId()));
        return invoice;
    }

    @Transactional
    public Invoice transition(UUID invoiceId, InvoiceStatus targetStatus, String reason) {
        Invoice invoice = getOwnedById(invoiceId);
        Set<InvoiceStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(invoice.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw InvalidStateTransitionException.of("Invoice", invoice.getStatus(), targetStatus);
        }
        InvoiceStatus previous = invoice.getStatus();
        invoice.setStatus(targetStatus);
        invoiceRepository.save(invoice);
        recordHistory(invoice, previous, targetStatus, reason);
        return invoice;
    }

    private void recordHistory(Invoice invoice, InvoiceStatus from, InvoiceStatus to, String reason) {
        InvoiceStatusHistory history = new InvoiceStatusHistory();
        history.setOrganizationId(invoice.getOrganizationId());
        history.setInvoiceId(invoice.getId());
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setReason(reason);
        statusHistoryRepository.save(history);
    }

    /** Simple carrier for create() so the controller knows whether to return 201 vs 200 (idempotent replay). */
    public record InvoiceCreationOutcome(Invoice invoice, int httpStatus, boolean replayed) {}

    /** Query-parameter filter bundle for {@link #search}. */
    public record InvoiceSearchFilter(
            UUID vendorId,
            InvoiceStatus status,
            InvoiceRiskLevel riskLevel,
            String purchaseOrderNumber,
            String createdBy,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            java.time.LocalDate invoiceDateFrom,
            java.time.LocalDate invoiceDateTo,
            java.time.LocalDate dueDateFrom,
            java.time.LocalDate dueDateTo,
            String keyword) {}
}

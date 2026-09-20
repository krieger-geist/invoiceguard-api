package com.invoiceguard.config.seed;

import com.invoiceguard.common.enums.CurrencyCode;
import com.invoiceguard.invoice.dto.InvoiceCreateRequest;
import com.invoiceguard.invoice.service.InvoiceService;
import com.invoiceguard.organization.entity.Organization;
import com.invoiceguard.organization.service.OrganizationMemberService;
import com.invoiceguard.organization.service.OrganizationService;
import com.invoiceguard.security.AuthenticatedPrincipal;
import com.invoiceguard.security.Role;
import com.invoiceguard.security.RolePermissions;
import com.invoiceguard.user.entity.User;
import com.invoiceguard.user.entity.UserStatus;
import com.invoiceguard.user.repository.UserRepository;
import com.invoiceguard.vendor.dto.BankAccountSubmissionRequest;
import com.invoiceguard.vendor.dto.VendorCreateRequest;
import com.invoiceguard.vendor.entity.BankAccountType;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import com.invoiceguard.vendor.service.VendorBankAccountService;
import com.invoiceguard.vendor.service.VendorService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populates a realistic demo dataset — one organisation, three users
 * (admin/analyst/reviewer), two vendors (one verified with an approved bank
 * account, one unverified), and a handful of invoices covering the normal,
 * exact-duplicate, and "obviously suspicious" cases your spec calls for.
 *
 * <p>Deliberately never enabled outside {@code dev} — gated by both
 * {@code @Profile("dev")} AND {@code invoiceguard.seed.enabled}, which
 * defaults to {@code false} everywhere except {@code application-dev.yml}.
 * Idempotent: checks for the demo admin email first and does nothing if
 * seed data already exists, so restarting the dev server repeatedly doesn't
 * duplicate everything.
 *
 * <p>Seeded invoices are left in {@code SUBMITTED} status rather than
 * pre-analysed — the point of a demo is to actually click "Analyse" in
 * Swagger and watch the risk engine produce findings live, not to stare at
 * numbers that were already computed for you.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "Demo1234!";

    private final boolean seedEnabled;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationService organizationService;
    private final OrganizationMemberService organizationMemberService;
    private final VendorService vendorService;
    private final VendorBankAccountService vendorBankAccountService;
    private final InvoiceService invoiceService;

    public DataSeeder(
            @Value("${invoiceguard.seed.enabled:false}") boolean seedEnabled,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            OrganizationService organizationService,
            OrganizationMemberService organizationMemberService,
            VendorService vendorService,
            VendorBankAccountService vendorBankAccountService,
            InvoiceService invoiceService) {
        this.seedEnabled = seedEnabled;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.organizationService = organizationService;
        this.organizationMemberService = organizationMemberService;
        this.vendorService = vendorService;
        this.vendorBankAccountService = vendorBankAccountService;
        this.invoiceService = invoiceService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase("admin@demo.invoiceguard.io")) {
            log.info("Seed data already present — skipping.");
            return;
        }

        log.info("Seeding demo data...");

        Organization organization = organizationService.create("Demo Organization", "trial");

        User admin = createUser("admin@demo.invoiceguard.io", "Ada", "Admin");
        User analyst = createUser("analyst@demo.invoiceguard.io", "Alex", "Analyst");
        User reviewer = createUser("reviewer@demo.invoiceguard.io", "Rae", "Reviewer");

        organizationMemberService.addExistingUserWithRole(organization.getId(), admin.getId(), Role.ORGANIZATION_ADMIN);
        organizationMemberService.addExistingUserWithRole(organization.getId(), analyst.getId(), Role.ANALYST);
        organizationMemberService.addExistingUserWithRole(organization.getId(), reviewer.getId(), Role.REVIEWER);

        runAs(admin, organization.getId(), Role.ORGANIZATION_ADMIN, () -> {
            Vendor verifiedVendor = createVerifiedVendorWithBankAccount();
            Vendor unverifiedVendor = createUnverifiedVendor();
            seedInvoices(verifiedVendor, unverifiedVendor);
            return null;
        });

        log.info("Seed data created. Demo login: admin@demo.invoiceguard.io / {}", DEMO_PASSWORD);
        log.info("Also seeded: analyst@demo.invoiceguard.io and reviewer@demo.invoiceguard.io, same password.");
    }

    private User createUser(String email, String firstName, String lastName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private Vendor createVerifiedVendorWithBankAccount() {
        Vendor vendor = vendorService.create(new VendorCreateRequest(
                "VEN-1001", "Northwind Office Supplies Ltd", "Northwind Supplies",
                "billing@northwindsupplies.com", "+1-555-0101", "500 Market St, San Francisco, CA", "US",
                "TAX-88213", "GST-44219"));
        vendorService.changeVerificationStatus(vendor.getId(), VendorVerificationStatus.VERIFIED);

        var changeRequest = vendorBankAccountService.submitBankAccount(
                vendor.getId(),
                new BankAccountSubmissionRequest(
                        "Northwind Office Supplies Ltd", "000123456789", "First National Bank", "Downtown Branch",
                        "FNBKUS31", BankAccountType.CHECKING, "Initial bank account on file"),
                currentUserId());
        vendorBankAccountService.decide(vendor.getId(), changeRequest.getId(), true, "Verified at onboarding", currentUserId());

        return vendorService.getOwnedById(vendor.getId());
    }

    private Vendor createUnverifiedVendor() {
        return vendorService.create(new VendorCreateRequest(
                "VEN-1002", "QuickFix IT Services", "QuickFix IT",
                "accounts@quickfixit-services.net", "+1-555-0199", "12 Industrial Way, Reno, NV", "US", null, null));
    }

    private void seedInvoices(Vendor verifiedVendor, Vendor unverifiedVendor) {
        // A normal, unremarkable invoice — should score LOW risk when analysed.
        create(verifiedVendor.getId(), "NW-2026-0501", LocalDate.of(2026, 5, 15), "PO-55012",
                "Office furniture — Q2 restock", "4250.00", "382.50", "4632.50");

        // Two invoices set up as an exact duplicate pair — same vendor, number, amount, date.
        create(verifiedVendor.getId(), "NW-2026-0502", LocalDate.of(2026, 6, 1), "PO-55030",
                "Printer toner cartridges (bulk)", "890.00", "80.10", "970.10");
        create(verifiedVendor.getId(), "NW-2026-0502", LocalDate.of(2026, 6, 1), "PO-55030",
                "Printer toner cartridges (bulk)", "890.00", "80.10", "970.10");

        // A "suspicious" invoice: unverified vendor, round amount, no PO number, free-email-style domain.
        create(unverifiedVendor.getId(), "QF-9981", LocalDate.of(2026, 6, 10), null,
                "IT consulting services", "5000.00", "0.00", "5000.00");
    }

    private void create(
            java.util.UUID vendorId, String invoiceNumber, LocalDate invoiceDate, String poNumber, String description,
            String subtotal, String tax, String total) {
        InvoiceCreateRequest request = new InvoiceCreateRequest(
                invoiceNumber, vendorId, invoiceDate, invoiceDate.plusDays(30), CurrencyCode.USD,
                new BigDecimal(subtotal), new BigDecimal(tax), BigDecimal.ZERO, new BigDecimal(total), poNumber, null,
                description, List.of());
        invoiceService.create(request, null);
    }

    private java.util.UUID currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).userId();
    }

    /**
     * Temporarily installs a security context so the normal, tenant-aware
     * service layer (which reads the caller from {@code TenantContext}) can
     * be reused as-is for seeding, instead of duplicating vendor/invoice
     * creation logic here bypassing all the validation those services do.
     */
    private <T> T runAs(User user, java.util.UUID organizationId, Role role, Supplier<T> action) {
        AuthenticatedPrincipal principal =
                new AuthenticatedPrincipal(user.getId(), user.getEmail(), organizationId, role, RolePermissions.permissionsFor(role));
        List<GrantedAuthority> authorities = principal.permissions().stream()
                .map(p -> (GrantedAuthority) new SimpleGrantedAuthority(p.getAuthority()))
                .toList();
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        var previous = SecurityContextHolder.getContext().getAuthentication();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            return action.get();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}

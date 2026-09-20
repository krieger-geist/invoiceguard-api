package com.invoiceguard.risk.service;

import com.invoiceguard.integration.ai.GeminiRiskExplanationService;
import com.invoiceguard.integration.ai.RuleBasedRiskExplanationService;
import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.invoice.entity.InvoiceRiskLevel;
import com.invoiceguard.invoice.entity.InvoiceStatus;
import com.invoiceguard.invoice.repository.InvoiceRepository;
import com.invoiceguard.invoice.service.InvoiceService;
import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RecommendedAction;
import com.invoiceguard.risk.entity.RiskAssessment;
import com.invoiceguard.risk.entity.RiskAssessmentStatus;
import com.invoiceguard.risk.entity.RiskFinding;
import com.invoiceguard.risk.entity.RiskRuleCode;
import com.invoiceguard.risk.event.CriticalRiskDetectedEvent;
import com.invoiceguard.risk.event.InvoiceAnalysedEvent;
import com.invoiceguard.risk.repository.RiskAssessmentRepository;
import com.invoiceguard.risk.repository.RiskFindingRepository;
import com.invoiceguard.risk.rule.RiskContext;
import com.invoiceguard.risk.rule.RiskRule;
import com.invoiceguard.risk.rule.RuleFinding;
import com.invoiceguard.vendor.entity.BankChangeRequest;
import com.invoiceguard.vendor.entity.Vendor;
import com.invoiceguard.vendor.entity.VendorBankAccount;
import com.invoiceguard.vendor.entity.VendorRiskStatus;
import com.invoiceguard.vendor.repository.BankChangeRequestRepository;
import com.invoiceguard.vendor.repository.VendorBankAccountRepository;
import com.invoiceguard.vendor.service.VendorService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates one full risk analysis run: assembles a {@link RiskContext},
 * runs every registered {@link RiskRule} against it, applies
 * organisation-specific rule configuration, scores and bands the result,
 * persists a {@link RiskAssessment} with its {@link RiskFinding}s, and
 * writes the outcome back onto the invoice and vendor.
 *
 * <p>Engine version is bumped whenever rule logic or scoring changes in a
 * way that would make old and new assessments not directly comparable —
 * it's persisted per-assessment specifically so historical assessments
 * remain interpretable after the engine evolves.
 */
@Service
public class RiskEngine {

    private static final String ENGINE_VERSION = "1.0";

    private final List<RiskRule> rules;
    private final RiskEngineProperties properties;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskFindingRepository riskFindingRepository;
    private final RiskRuleConfigService riskRuleConfigService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final VendorStatisticsService vendorStatisticsService;
    private final InvoiceService invoiceService;
    private final InvoiceRepository invoiceRepository;
    private final VendorService vendorService;
    private final VendorBankAccountRepository vendorBankAccountRepository;
    private final BankChangeRequestRepository bankChangeRequestRepository;
    private final RuleBasedRiskExplanationService ruleBasedExplanationService;
    private final Optional<GeminiRiskExplanationService> aiExplanationService;
    private final ApplicationEventPublisher eventPublisher;

    public RiskEngine(
            List<RiskRule> rules,
            RiskEngineProperties properties,
            RiskAssessmentRepository riskAssessmentRepository,
            RiskFindingRepository riskFindingRepository,
            RiskRuleConfigService riskRuleConfigService,
            DuplicateDetectionService duplicateDetectionService,
            VendorStatisticsService vendorStatisticsService,
            InvoiceService invoiceService,
            InvoiceRepository invoiceRepository,
            VendorService vendorService,
            VendorBankAccountRepository vendorBankAccountRepository,
            BankChangeRequestRepository bankChangeRequestRepository,
            RuleBasedRiskExplanationService ruleBasedExplanationService,
            Optional<GeminiRiskExplanationService> aiExplanationService,
            ApplicationEventPublisher eventPublisher) {
        this.rules = rules;
        this.properties = properties;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskFindingRepository = riskFindingRepository;
        this.riskRuleConfigService = riskRuleConfigService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.vendorStatisticsService = vendorStatisticsService;
        this.invoiceService = invoiceService;
        this.invoiceRepository = invoiceRepository;
        this.vendorService = vendorService;
        this.vendorBankAccountRepository = vendorBankAccountRepository;
        this.bankChangeRequestRepository = bankChangeRequestRepository;
        this.ruleBasedExplanationService = ruleBasedExplanationService;
        this.aiExplanationService = aiExplanationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Runs a full analysis for an invoice currently in {@code SUBMITTED}
     * status: transitions it through {@code ANALYSING} to
     * {@code REVIEW_REQUIRED}, and returns the persisted assessment.
     */
    @Transactional
    public RiskAssessment analyse(UUID invoiceId) {
        long start = System.currentTimeMillis();

        Invoice invoice = invoiceService.transition(invoiceId, InvoiceStatus.ANALYSING, "Risk analysis started");
        RiskContext context = buildContext(invoice);

        Map<RiskRuleCode, RuleConfigSnapshot> configByCode = riskRuleConfigService.getEffectiveConfigSnapshot(invoice.getOrganizationId());

        List<RiskFinding> findings = new ArrayList<>();
        int totalScore = 0;

        for (RiskRule rule : rules) {
            RuleConfigSnapshot config = configByCode.get(rule.getCode());
            boolean enabled = config == null || config.enabled();
            if (!enabled) {
                continue;
            }

            Optional<RuleFinding> result = rule.evaluate(context);
            if (result.isEmpty()) {
                continue;
            }

            RuleFinding ruleFinding = result.get();
            int points = (config != null && config.weightPoints() != null)
                    ? config.weightPoints()
                    : ruleFinding.basePoints();
            totalScore += points;

            RiskFinding finding = new RiskFinding();
            finding.setOrganizationId(invoice.getOrganizationId());
            finding.setInvoiceId(invoice.getId());
            finding.setRuleCode(rule.getCode());
            finding.setTitle(ruleFinding.title());
            finding.setDescription(ruleFinding.description());
            finding.setSeverity(ruleFinding.severity());
            finding.setPoints(points);
            finding.setEvidence(ruleFinding.evidenceJson());
            findings.add(finding);
        }

        totalScore = Math.min(100, totalScore);
        InvoiceRiskLevel riskLevel = bandLevel(totalScore);
        RecommendedAction action = recommendAction(riskLevel, findings);
        String explanation = ruleBasedExplanationService.explain(invoice, findings);
        String aiSummary = aiExplanationService.map(service -> service.explain(invoice, findings)).orElse(null);

        RiskAssessment assessment = new RiskAssessment();
        assessment.setOrganizationId(invoice.getOrganizationId());
        assessment.setInvoiceId(invoice.getId());
        assessment.setTotalRiskScore(totalScore);
        assessment.setRiskLevel(riskLevel);
        assessment.setRecommendedAction(action);
        assessment.setAnalysedAt(Instant.now());
        assessment.setEngineVersion(ENGINE_VERSION);
        assessment.setProcessingDurationMs(System.currentTimeMillis() - start);
        assessment.setStatus(RiskAssessmentStatus.COMPLETED);
        assessment.setExplanation(explanation);
        assessment.setAiSummary(aiSummary);
        assessment = riskAssessmentRepository.save(assessment);

        for (RiskFinding finding : findings) {
            finding.setRiskAssessmentId(assessment.getId());
        }
        riskFindingRepository.saveAll(findings);

        invoiceService.applyRiskResult(invoice.getId(), riskLevel, totalScore);
        vendorService.updateRiskStatus(context.vendor().getId(), VendorRiskStatus.valueOf(riskLevel.name()));
        invoiceService.transition(invoice.getId(), InvoiceStatus.REVIEW_REQUIRED, "Risk analysis completed");

        eventPublisher.publishEvent(new InvoiceAnalysedEvent(
                invoice.getId(), invoice.getOrganizationId(), totalScore, riskLevel, action, !context.exactDuplicates().isEmpty()));
        if (riskLevel == InvoiceRiskLevel.CRITICAL) {
            eventPublisher.publishEvent(new CriticalRiskDetectedEvent(invoice.getId(), invoice.getOrganizationId(), totalScore));
        }

        return assessment;
    }

    private RiskContext buildContext(Invoice invoice) {
        UUID orgId = invoice.getOrganizationId();
        UUID vendorId = invoice.getVendorId();

        Vendor vendor = vendorService.getOwnedById(vendorId);
        List<VendorBankAccount> bankAccounts = vendorBankAccountRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);

        Instant bankChangeWindowStart = Instant.now().minus(properties.recentBankChangeWindowDays(), ChronoUnit.DAYS);
        List<BankChangeRequest> bankChangeRequests = bankChangeRequestRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
        int recentBankChangeCount = (int) bankChangeRequests.stream()
                .filter(r -> r.getCreatedAt().isAfter(bankChangeWindowStart))
                .count();

        VendorStatistics stats = vendorStatisticsService.computeFor(orgId, vendorId, invoice.getId());

        List<DuplicateMatch> exactDuplicates = duplicateDetectionService.findExactDuplicates(invoice);
        List<DuplicateMatch> nearDuplicates = duplicateDetectionService.findNearDuplicates(invoice);

        boolean duplicatePo = invoice.getPurchaseOrderNumber() != null
                && !invoice.getPurchaseOrderNumber().isBlank()
                && !invoiceRepository
                        .findByOrganizationIdAndPurchaseOrderNumberAndIdNot(orgId, invoice.getPurchaseOrderNumber(), invoice.getId())
                        .isEmpty();

        boolean duplicatePaymentRef = invoice.getPaymentReference() != null
                && !invoice.getPaymentReference().isBlank()
                && !invoiceRepository
                        .findByOrganizationIdAndPaymentReferenceAndIdNot(orgId, invoice.getPaymentReference(), invoice.getId())
                        .isEmpty();

        Instant rapidRepeatWindowStart = Instant.now().minus(properties.rapidRepeatWindowMinutes(), ChronoUnit.MINUTES);
        boolean rapidRepeat = !invoiceRepository
                .findByOrganizationIdAndVendorIdAndCreatedAtAfterAndIdNot(orgId, vendorId, rapidRepeatWindowStart, invoice.getId())
                .isEmpty();

        List<Invoice> vendorHistory = invoiceRepository.findByOrganizationIdAndVendorIdOrderByInvoiceDateDesc(orgId, vendorId).stream()
                .filter(inv -> !inv.getId().equals(invoice.getId()))
                .toList();
        List<String> recentDescriptions = vendorHistory.stream()
                .map(Invoice::getDescription)
                .filter(d -> d != null && !d.isBlank())
                .limit(5)
                .toList();

        return new RiskContext(
                invoice,
                invoiceService.getItems(invoice.getId()),
                vendor,
                bankAccounts,
                recentBankChangeCount,
                stats,
                exactDuplicates,
                nearDuplicates,
                duplicatePo,
                duplicatePaymentRef,
                rapidRepeat,
                vendorHistory.isEmpty(),
                recentDescriptions);
    }

    private InvoiceRiskLevel bandLevel(int score) {
        var thresholds = properties.thresholds();
        if (score <= thresholds.lowMax()) return InvoiceRiskLevel.LOW;
        if (score <= thresholds.mediumMax()) return InvoiceRiskLevel.MEDIUM;
        if (score <= thresholds.highMax()) return InvoiceRiskLevel.HIGH;
        return InvoiceRiskLevel.CRITICAL;
    }

    private RecommendedAction recommendAction(InvoiceRiskLevel level, List<RiskFinding> findings) {
        if (level == InvoiceRiskLevel.CRITICAL) {
            return RecommendedAction.BLOCK_PAYMENT;
        }
        boolean vendorNeedsVerification = findings.stream().anyMatch(f -> f.getRuleCode() == RiskRuleCode.UNVERIFIED_VENDOR);
        if (vendorNeedsVerification) {
            return RecommendedAction.REQUEST_VENDOR_VERIFICATION;
        }
        return switch (level) {
            case HIGH -> RecommendedAction.MANUAL_REVIEW;
            case MEDIUM -> RecommendedAction.STANDARD_REVIEW;
            default -> RecommendedAction.AUTO_APPROVE_ELIGIBLE;
        };
    }
}

package com.invoiceguard.analytics.service;

import com.invoiceguard.analytics.dto.AnalyticsSummaryResponse;
import com.invoiceguard.analytics.dto.MonthlyTrendResponse;
import com.invoiceguard.analytics.dto.RiskDistributionResponse;
import com.invoiceguard.analytics.dto.RiskDistributionResponse.RuleFrequency;
import com.invoiceguard.analytics.dto.VendorRiskSummaryResponse;
import com.invoiceguard.security.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Every query here is hand-written SQL via {@link JdbcTemplate} rather than
 * a JPA repository method — the spec's "use DTO projections or optimized
 * queries to avoid loading complete entities" is most literally true when
 * there's no entity hydration happening at all: these are pure aggregate
 * reads (COUNT/SUM/AVG/GROUP BY) that never need a Hibernate-managed object
 * graph, so going straight to SQL avoids that overhead entirely.
 *
 * <p>Every query is manually scoped to {@code organization_id = ?} — since
 * these bypass Spring Data JPA entirely, they also bypass any entity-level
 * tenant-safety pattern, so getting the WHERE clause right here is the only
 * thing standing between one org and another's analytics. Every method
 * takes {@code organizationId} as an explicit first parameter for exactly
 * this reason, rather than reading it once at the top of the class.
 */
@Service
public class AnalyticsService {

    private final JdbcTemplate jdbcTemplate;
    private final TenantContext tenantContext;
    private final int approvalOverdueHours;

    public AnalyticsService(
            JdbcTemplate jdbcTemplate,
            TenantContext tenantContext,
            @Value("${invoiceguard.approval.overdue-hours:48}") int approvalOverdueHours) {
        this.jdbcTemplate = jdbcTemplate;
        this.tenantContext = tenantContext;
        this.approvalOverdueHours = approvalOverdueHours;
    }

    public AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to) {
        UUID organizationId = tenantContext.requireOrganizationId();
        LocalDate[] range = defaultRange(from, to);

        long total = countInvoices(organizationId, range, null);
        long approved = countInvoices(organizationId, range, "APPROVED");
        long rejected = countInvoices(organizationId, range, "REJECTED");
        long underReview = countInvoices(organizationId, range, "REVIEW_REQUIRED");

        long pendingApprovals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_requests WHERE organization_id = ? AND status = 'PENDING'",
                Long.class, organizationId);

        Instant overdueThreshold = Instant.now().minus(approvalOverdueHours, ChronoUnit.HOURS);
        long overdueApprovals = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM approval_requests WHERE organization_id = ? AND status = 'PENDING' AND created_at < ?",
                Long.class, organizationId, java.sql.Timestamp.from(overdueThreshold));

        long duplicatesFound = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT invoice_id) FROM risk_findings WHERE organization_id = ? "
                        + "AND rule_code IN ('EXACT_DUPLICATE','NEAR_DUPLICATE')",
                Long.class, organizationId);

        BigDecimal preventedDuplicateAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE organization_id = ? AND id IN "
                        + "(SELECT DISTINCT invoice_id FROM risk_findings WHERE organization_id = ? AND rule_code = 'EXACT_DUPLICATE')",
                BigDecimal.class, organizationId, organizationId);

        BigDecimal estimatedPreventedLoss = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM invoices WHERE organization_id = ? "
                        + "AND risk_level = 'CRITICAL' AND status = 'REJECTED'",
                BigDecimal.class, organizationId);

        Double averageApprovalHours = jdbcTemplate.queryForObject(
                "SELECT AVG(EXTRACT(EPOCH FROM (decided_at - created_at)) / 3600.0) FROM approval_requests "
                        + "WHERE organization_id = ? AND decided_at IS NOT NULL AND created_at BETWEEN ? AND ?",
                Double.class, organizationId, startOfDay(range[0]), endOfDay(range[1]));

        return new AnalyticsSummaryResponse(
                total, approved, rejected, underReview, pendingApprovals, overdueApprovals, duplicatesFound,
                preventedDuplicateAmount, estimatedPreventedLoss, averageApprovalHours);
    }

    public RiskDistributionResponse getRiskDistribution(LocalDate from, LocalDate to) {
        UUID organizationId = tenantContext.requireOrganizationId();
        LocalDate[] range = defaultRange(from, to);

        Map<String, Long> byLevel = new LinkedHashMap<>();
        jdbcTemplate.query(
                        "SELECT risk_level, COUNT(*) c FROM invoices WHERE organization_id = ? AND risk_level IS NOT NULL "
                                + "AND invoice_date BETWEEN ? AND ? GROUP BY risk_level ORDER BY c DESC",
                        (rs, rowNum) -> Map.entry(rs.getString("risk_level"), rs.getLong("c")),
                        organizationId, range[0], range[1])
                .forEach(e -> byLevel.put(e.getKey(), e.getValue()));

        List<RuleFrequency> topRules = jdbcTemplate.query(
                "SELECT rule_code, COUNT(*) c FROM risk_findings WHERE organization_id = ? "
                        + "GROUP BY rule_code ORDER BY c DESC LIMIT 10",
                (rs, rowNum) -> new RuleFrequency(rs.getString("rule_code"), rs.getLong("c")),
                organizationId);

        return new RiskDistributionResponse(byLevel, topRules);
    }

    public List<VendorRiskSummaryResponse> getVendorRisk() {
        UUID organizationId = tenantContext.requireOrganizationId();
        return jdbcTemplate.query(
                "SELECT v.id vendor_id, v.vendor_code, v.risk_status, COUNT(i.id) invoice_count, "
                        + "COALESCE(SUM(i.total_amount), 0) total_amount "
                        + "FROM vendors v LEFT JOIN invoices i ON i.vendor_id = v.id AND i.deleted = false "
                        + "WHERE v.organization_id = ? "
                        + "GROUP BY v.id, v.vendor_code, v.risk_status "
                        + "ORDER BY total_amount DESC",
                (rs, rowNum) -> new VendorRiskSummaryResponse(
                        UUID.fromString(rs.getString("vendor_id")),
                        rs.getString("vendor_code"),
                        rs.getString("risk_status"),
                        rs.getLong("invoice_count"),
                        rs.getBigDecimal("total_amount")),
                organizationId);
    }

    public List<MonthlyTrendResponse> getMonthlyTrends(LocalDate from, LocalDate to) {
        UUID organizationId = tenantContext.requireOrganizationId();
        LocalDate[] range = defaultRange(from, to);
        return jdbcTemplate.query(
                "SELECT to_char(date_trunc('month', invoice_date), 'YYYY-MM') ym, COUNT(*) c, COALESCE(SUM(total_amount), 0) s "
                        + "FROM invoices WHERE organization_id = ? AND deleted = false AND invoice_date BETWEEN ? AND ? "
                        + "GROUP BY ym ORDER BY ym",
                (rs, rowNum) -> new MonthlyTrendResponse(rs.getString("ym"), rs.getLong("c"), rs.getBigDecimal("s")),
                organizationId, range[0], range[1]);
    }

    private long countInvoices(UUID organizationId, LocalDate[] range, String status) {
        String sql = "SELECT COUNT(*) FROM invoices WHERE organization_id = ? AND deleted = false "
                + "AND invoice_date BETWEEN ? AND ?" + (status != null ? " AND status = ?" : "");
        return status != null
                ? jdbcTemplate.queryForObject(sql, Long.class, organizationId, range[0], range[1], status)
                : jdbcTemplate.queryForObject(sql, Long.class, organizationId, range[0], range[1]);
    }

    private LocalDate[] defaultRange(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        LocalDate resolvedFrom = from != null ? from : resolvedTo.minusMonths(3);
        return new LocalDate[] {resolvedFrom, resolvedTo};
    }

    private java.sql.Timestamp startOfDay(LocalDate date) {
        return java.sql.Timestamp.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private java.sql.Timestamp endOfDay(LocalDate date) {
        return java.sql.Timestamp.from(date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}

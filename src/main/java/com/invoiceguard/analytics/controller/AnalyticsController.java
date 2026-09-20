package com.invoiceguard.analytics.controller;

import com.invoiceguard.analytics.dto.AnalyticsSummaryResponse;
import com.invoiceguard.analytics.dto.MonthlyTrendResponse;
import com.invoiceguard.analytics.dto.RiskDistributionResponse;
import com.invoiceguard.analytics.dto.VendorRiskSummaryResponse;
import com.invoiceguard.analytics.service.AnalyticsService;
import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAuthority('analytics:read')")

@Tag(name = "Analytics", description = "Aggregate reporting: invoice/approval summary, risk distribution, vendor risk, monthly trends.")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ApiResponse<AnalyticsSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(analyticsService.getSummary(from, to), correlationId(httpRequest));
    }

    @GetMapping("/risk-distribution")
    public ApiResponse<RiskDistributionResponse> riskDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(analyticsService.getRiskDistribution(from, to), correlationId(httpRequest));
    }

    @GetMapping("/vendor-risk")
    public ApiResponse<List<VendorRiskSummaryResponse>> vendorRisk(HttpServletRequest httpRequest) {
        return ApiResponse.ok(analyticsService.getVendorRisk(), correlationId(httpRequest));
    }

    @GetMapping("/monthly-trends")
    public ApiResponse<List<MonthlyTrendResponse>> monthlyTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(analyticsService.getMonthlyTrends(from, to), correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}

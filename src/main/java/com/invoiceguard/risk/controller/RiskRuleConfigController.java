package com.invoiceguard.risk.controller;

import com.invoiceguard.common.response.ApiResponse;
import com.invoiceguard.config.CorrelationIdFilter;
import com.invoiceguard.risk.dto.RiskRuleConfigResponse;
import com.invoiceguard.risk.dto.RiskRuleConfigUpdateRequest;
import com.invoiceguard.risk.entity.RiskRuleCode;
import com.invoiceguard.risk.service.RiskRuleConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/risk-rules")


@Tag(name = "Risk Rules", description = "View/customize the risk engine's 21 rules per organisation (enable, disable, reweight).")
public class RiskRuleConfigController {

    private final RiskRuleConfigService service;

    public RiskRuleConfigController(RiskRuleConfigService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('risk:configure')")
    public ApiResponse<List<RiskRuleConfigResponse>> list(HttpServletRequest httpRequest) {
        return ApiResponse.ok(service.listAll(), correlationId(httpRequest));
    }

    @PutMapping("/{ruleCode}")
    @PreAuthorize("hasAuthority('risk:configure')")
    public ApiResponse<RiskRuleConfigResponse> update(
            @PathVariable RiskRuleCode ruleCode,
            @Valid @RequestBody RiskRuleConfigUpdateRequest request,
            HttpServletRequest httpRequest) {
        RiskRuleConfigResponse response = service.update(ruleCode, request.enabled(), request.weightPoints());
        return ApiResponse.of("Risk rule configuration updated", response, correlationId(httpRequest));
    }

    private String correlationId(HttpServletRequest request) {
        Object attr = request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        return attr != null ? attr.toString() : null;
    }
}

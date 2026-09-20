package com.invoiceguard.risk.mapper;

import com.invoiceguard.risk.dto.RiskAssessmentResponse;
import com.invoiceguard.risk.dto.RiskFindingResponse;
import com.invoiceguard.risk.entity.RiskAssessment;
import com.invoiceguard.risk.entity.RiskFinding;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RiskAssessmentMapper {

    RiskFindingResponse toResponse(RiskFinding finding);

    default RiskAssessmentResponse toResponse(RiskAssessment assessment, List<RiskFinding> findings) {
        List<RiskFindingResponse> findingResponses = findings.stream().map(this::toResponse).toList();
        return new RiskAssessmentResponse(
                assessment.getId(),
                assessment.getInvoiceId(),
                assessment.getTotalRiskScore(),
                assessment.getRiskLevel(),
                assessment.getRecommendedAction(),
                assessment.getStatus(),
                assessment.getExplanation(),
                assessment.getAiSummary(),
                assessment.getEngineVersion(),
                assessment.getProcessingDurationMs(),
                assessment.getAnalysedAt(),
                findingResponses);
    }
}

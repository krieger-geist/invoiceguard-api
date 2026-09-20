package com.invoiceguard.risk.service;

import com.invoiceguard.exception.ResourceNotFoundException;
import com.invoiceguard.risk.entity.RiskAssessment;
import com.invoiceguard.risk.entity.RiskFinding;
import com.invoiceguard.risk.repository.RiskAssessmentRepository;
import com.invoiceguard.risk.repository.RiskFindingRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskAssessmentQueryService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskFindingRepository riskFindingRepository;

    public RiskAssessmentQueryService(
            RiskAssessmentRepository riskAssessmentRepository, RiskFindingRepository riskFindingRepository) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskFindingRepository = riskFindingRepository;
    }

    @Transactional(readOnly = true)
    public RiskAssessment getLatestForInvoice(UUID invoiceId) {
        return riskAssessmentRepository
                .findTopByInvoiceIdOrderByAnalysedAtDesc(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No risk assessment has been run for this invoice yet"));
    }

    @Transactional(readOnly = true)
    public List<RiskFinding> getFindings(UUID riskAssessmentId) {
        return riskFindingRepository.findByRiskAssessmentIdOrderByPointsDesc(riskAssessmentId);
    }
}

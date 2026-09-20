package com.invoiceguard.risk.repository;

import com.invoiceguard.risk.entity.RiskAssessment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    Optional<RiskAssessment> findTopByInvoiceIdOrderByAnalysedAtDesc(UUID invoiceId);

    Optional<RiskAssessment> findByIdAndOrganizationId(UUID id, UUID organizationId);
}

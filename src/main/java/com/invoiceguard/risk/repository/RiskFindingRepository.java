package com.invoiceguard.risk.repository;

import com.invoiceguard.risk.entity.RiskFinding;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskFindingRepository extends JpaRepository<RiskFinding, UUID> {

    List<RiskFinding> findByRiskAssessmentIdOrderByPointsDesc(UUID riskAssessmentId);
}

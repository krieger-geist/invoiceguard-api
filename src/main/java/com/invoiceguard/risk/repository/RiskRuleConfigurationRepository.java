package com.invoiceguard.risk.repository;

import com.invoiceguard.risk.entity.RiskRuleCode;
import com.invoiceguard.risk.entity.RiskRuleConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleConfigurationRepository extends JpaRepository<RiskRuleConfiguration, UUID> {

    List<RiskRuleConfiguration> findByOrganizationId(UUID organizationId);

    Optional<RiskRuleConfiguration> findByOrganizationIdAndRuleCode(UUID organizationId, RiskRuleCode ruleCode);
}

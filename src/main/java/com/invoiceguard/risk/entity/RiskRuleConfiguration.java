package com.invoiceguard.risk.entity;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Per-organisation override of a rule's enabled state and point weight. Only
 * rules an organisation has explicitly customized get a row here — rules
 * with no row simply use their built-in default weight and are enabled by
 * default (see {@code RiskEngine.resolveEffectiveConfig}), so a brand-new
 * organisation doesn't need this table seeded with 21 rows before analysis works.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "risk_rule_configurations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "rule_code"}))
public class RiskRuleConfiguration extends OrganizationScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_code", nullable = false, length = 40)
    private RiskRuleCode ruleCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "weight_points")
    private Integer weightPoints;
}

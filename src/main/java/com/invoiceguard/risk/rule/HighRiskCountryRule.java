package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** {@code invoiceguard.risk.high-risk-countries} is empty by default — each organisation configures its own list per its compliance policy. */
@Component
public class HighRiskCountryRule implements RiskRule {

    private final RiskEngineProperties properties;

    public HighRiskCountryRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.HIGH_RISK_COUNTRY;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        String country = context.vendor().getCountry();
        if (country == null || properties.highRiskCountries() == null
                || !properties.highRiskCountries().contains(country.toUpperCase())) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Vendor located in a configured high-risk jurisdiction",
                "This vendor's registered country (" + country + ") is on this organisation's high-risk country list.",
                RiskFindingSeverity.MEDIUM,
                10,
                "{\"country\":\"" + country + "\"}"));
    }
}

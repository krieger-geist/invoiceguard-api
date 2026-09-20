package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class VendorRecentlyCreatedRule implements RiskRule {

    private final RiskEngineProperties properties;

    public VendorRecentlyCreatedRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.VENDOR_RECENTLY_CREATED;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        long ageDays = ChronoUnit.DAYS.between(context.vendor().getCreatedAt(), Instant.now());
        if (ageDays > properties.vendorRecentlyCreatedDays()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Vendor record recently created",
                "This vendor was added to the system only " + ageDays + " day(s) ago.",
                RiskFindingSeverity.LOW,
                8,
                "{\"vendorAgeDays\":" + ageDays + "}"));
    }
}

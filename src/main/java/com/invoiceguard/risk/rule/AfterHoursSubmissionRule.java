package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AfterHoursSubmissionRule implements RiskRule {

    private final RiskEngineProperties properties;

    public AfterHoursSubmissionRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.AFTER_HOURS_SUBMISSION;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        int hour = context.invoice().getCreatedAt().atZone(ZoneOffset.UTC).getHour();
        if (hour >= properties.businessHourStart() && hour < properties.businessHourEnd()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Submitted outside business hours",
                "This invoice was submitted at " + hour + ":00 UTC, outside the configured business hours window.",
                RiskFindingSeverity.LOW,
                5,
                "{\"hourUtc\":" + hour + "}"));
    }
}

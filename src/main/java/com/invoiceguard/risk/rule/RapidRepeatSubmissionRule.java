package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RapidRepeatSubmissionRule implements RiskRule {

    private final RiskEngineProperties properties;

    public RapidRepeatSubmissionRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.RAPID_REPEAT_SUBMISSION;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (!context.rapidRepeatSubmissionExists()) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Rapid repeat submission from this vendor",
                "This vendor submitted another invoice within the last " + properties.rapidRepeatWindowMinutes()
                        + " minutes, an unusual submission cadence.",
                RiskFindingSeverity.MEDIUM,
                12,
                "{\"windowMinutes\":" + properties.rapidRepeatWindowMinutes() + "}"));
    }
}

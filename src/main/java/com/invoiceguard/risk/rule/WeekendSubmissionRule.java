package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.time.DayOfWeek;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class WeekendSubmissionRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.WEEKEND_SUBMISSION;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        DayOfWeek day = context.invoice().getCreatedAt().atZone(ZoneOffset.UTC).getDayOfWeek();
        if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Submitted on a weekend",
                "This invoice was submitted on a " + day + ", outside typical business submission patterns.",
                RiskFindingSeverity.LOW,
                5,
                "{\"dayOfWeek\":\"" + day + "\"}"));
    }
}

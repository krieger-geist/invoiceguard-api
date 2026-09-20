package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.config.RiskEngineProperties;
import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Flags vendors invoicing from free consumer email domains rather than a corporate domain — a common BEC/fake-vendor signal. */
@Component
public class SuspiciousVendorEmailRule implements RiskRule {

    private final RiskEngineProperties properties;

    public SuspiciousVendorEmailRule(RiskEngineProperties properties) {
        this.properties = properties;
    }

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.SUSPICIOUS_VENDOR_EMAIL;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        String email = context.vendor().getEmail();
        if (email == null || !email.contains("@")) {
            return Optional.empty();
        }
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (!properties.freeEmailDomains().contains(domain)) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Vendor uses a free consumer email domain",
                "This vendor's contact email (" + domain + ") is a free consumer email provider rather than a corporate domain.",
                RiskFindingSeverity.MEDIUM,
                10,
                "{\"emailDomain\":\"" + domain + "\"}"));
    }
}

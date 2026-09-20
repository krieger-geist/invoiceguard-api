package com.invoiceguard.risk.rule;

import com.invoiceguard.risk.entity.RiskFindingSeverity;
import com.invoiceguard.risk.entity.RiskRuleCode;
import com.invoiceguard.vendor.entity.VendorVerificationStatus;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UnverifiedVendorRule implements RiskRule {

    @Override
    public RiskRuleCode getCode() {
        return RiskRuleCode.UNVERIFIED_VENDOR;
    }

    @Override
    public Optional<RuleFinding> evaluate(RiskContext context) {
        if (context.vendor().getVerificationStatus() == VendorVerificationStatus.VERIFIED) {
            return Optional.empty();
        }
        return Optional.of(new RuleFinding(
                "Vendor is not verified",
                "Vendor verification status is " + context.vendor().getVerificationStatus()
                        + ". Payments to unverified vendors carry elevated fraud risk.",
                RiskFindingSeverity.MEDIUM,
                15,
                "{\"verificationStatus\":\"" + context.vendor().getVerificationStatus() + "\"}"));
    }
}

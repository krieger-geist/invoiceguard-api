package com.invoiceguard.integration.ai;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.risk.entity.RiskFinding;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * The default, always-on {@link RiskExplanationService}. Template-based and
 * fully deterministic — no external dependency, no latency, no possibility
 * of failure. This is what the risk engine relies on for its guaranteed
 * explanation; AI (Gemini) is additive on top of this, never a replacement.
 */
@Service
public class RuleBasedRiskExplanationService implements RiskExplanationService {

    @Override
    public String explain(Invoice invoice, List<RiskFinding> findings) {
        if (findings.isEmpty()) {
            return "No risk indicators were identified for invoice " + invoice.getInvoiceNumber()
                    + ". This invoice appears consistent with normal invoicing patterns.";
        }

        List<RiskFinding> sorted =
                findings.stream().sorted(Comparator.comparingInt(RiskFinding::getPoints).reversed()).toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Invoice ")
                .append(invoice.getInvoiceNumber())
                .append(" was flagged with ")
                .append(findings.size())
                .append(findings.size() == 1 ? " risk indicator: " : " risk indicators: ");

        for (int i = 0; i < sorted.size(); i++) {
            RiskFinding finding = sorted.get(i);
            sb.append(finding.getTitle()).append(" (").append(finding.getSeverity()).append(", +")
                    .append(finding.getPoints()).append(" pts)");
            sb.append(i < sorted.size() - 1 ? "; " : ". ");
        }

        sb.append("The highest-severity concern is: ").append(sorted.get(0).getDescription());
        return sb.toString();
    }
}

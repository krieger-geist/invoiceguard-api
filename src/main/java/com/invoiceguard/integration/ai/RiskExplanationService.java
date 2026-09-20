package com.invoiceguard.integration.ai;

import com.invoiceguard.invoice.entity.Invoice;
import com.invoiceguard.risk.entity.RiskFinding;
import java.util.List;

/**
 * Turns a list of {@link RiskFinding}s into a natural-language summary. Two
 * implementations exist: {@code RuleBasedRiskExplanationService} (always
 * available, deterministic, no network call — this is what actually backs
 * {@code RiskAssessment.explanation} so the "never return only a score"
 * requirement holds even with AI fully disabled) and
 * {@code GeminiRiskExplanationService} (optional, config-flagged, used only
 * to populate the supplementary {@code RiskAssessment.aiSummary} field —
 * see {@code RiskEngine} for how the two are combined).
 *
 * <p>Per the spec, AI implementations of this interface must never be given
 * the ability to change a risk decision — this interface's only method
 * returns a {@code String}, not a score, a recommendation, or an approval.
 */
public interface RiskExplanationService {

    String explain(Invoice invoice, List<RiskFinding> findings);
}

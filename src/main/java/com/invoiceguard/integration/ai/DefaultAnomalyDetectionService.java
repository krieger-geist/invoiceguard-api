package com.invoiceguard.integration.ai;

import com.invoiceguard.risk.service.VendorStatistics;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class DefaultAnomalyDetectionService implements AnomalyDetectionService {

    @Override
    public boolean isAnomalous(BigDecimal amount, VendorStatistics statistics, double zScoreThreshold) {
        if (statistics.stdDevAmount() == 0) {
            return false;
        }
        double zScore = Math.abs(amount.doubleValue() - statistics.averageAmount().doubleValue()) / statistics.stdDevAmount();
        return zScore >= zScoreThreshold;
    }
}

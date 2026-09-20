package com.invoiceguard.integration.ai;

import com.invoiceguard.common.util.StringSimilarity;
import org.springframework.stereotype.Service;

@Service
public class DefaultInvoiceSimilarityService implements InvoiceSimilarityService {

    @Override
    public double similarity(String textA, String textB) {
        return StringSimilarity.wordOverlapSimilarity(textA, textB);
    }
}

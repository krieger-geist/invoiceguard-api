package com.invoiceguard.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class StringSimilarityTest {

    @Test
    void identicalStringsHaveZeroEditDistance() {
        assertThat(StringSimilarity.levenshteinDistance("INV1001", "INV1001")).isZero();
    }

    @Test
    void singleCharacterDifferenceHasDistanceOne() {
        assertThat(StringSimilarity.levenshteinDistance("INV1001", "INV1002")).isEqualTo(1);
    }

    @Test
    void normalizedSimilarityOfIdenticalStringsIsOne() {
        assertThat(StringSimilarity.normalizedSimilarity("ABC123", "ABC123")).isEqualTo(1.0);
    }

    @Test
    void normalizedSimilarityOfCompletelyDifferentStringsIsLow() {
        double similarity = StringSimilarity.normalizedSimilarity("ABC123", "ZZZZZZ");
        assertThat(similarity).isLessThan(0.3);
    }

    @Test
    void normalizedSimilarityOfNearMissInvoiceNumbersIsHigh() {
        // The exact near-duplicate scenario the risk engine cares about: one transposed/changed digit.
        double similarity = StringSimilarity.normalizedSimilarity("INV1001", "INV1O01");
        assertThat(similarity).isCloseTo(0.857, within(0.01));
    }

    @Test
    void wordOverlapSimilarityIgnoresWordOrderAndCase() {
        double similarity = StringSimilarity.wordOverlapSimilarity(
                "Office chairs and desks", "Desks and office chairs");
        assertThat(similarity).isEqualTo(1.0);
    }

    @Test
    void wordOverlapSimilarityOfUnrelatedTextIsZero() {
        assertThat(StringSimilarity.wordOverlapSimilarity("office chairs", "software license")).isZero();
    }

    @Test
    void wordOverlapSimilarityHandlesBlankInput() {
        assertThat(StringSimilarity.wordOverlapSimilarity(null, "anything")).isZero();
        assertThat(StringSimilarity.wordOverlapSimilarity("", "anything")).isZero();
    }
}

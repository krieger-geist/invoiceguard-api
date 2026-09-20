package com.invoiceguard.invoice.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvoiceNumberNormalizerTest {

    @Test
    void stripsSpacesHyphensUnderscoresAndUppercases() {
        assertThat(InvoiceNumberNormalizer.normalize("inv-1001")).isEqualTo("INV1001");
        assertThat(InvoiceNumberNormalizer.normalize("INV 1001")).isEqualTo("INV1001");
        assertThat(InvoiceNumberNormalizer.normalize("inv_1001")).isEqualTo("INV1001");
        assertThat(InvoiceNumberNormalizer.normalize("INV#1001!")).isEqualTo("INV1001");
    }

    @Test
    void treatsDifferentlyFormattedNumbersAsIdentical() {
        String a = InvoiceNumberNormalizer.normalize("INV-1001");
        String b = InvoiceNumberNormalizer.normalize("inv 1001");
        String c = InvoiceNumberNormalizer.normalize("Inv_1001");
        assertThat(a).isEqualTo(b).isEqualTo(c);
    }

    @Test
    void handlesNullAndBlankGracefully() {
        assertThat(InvoiceNumberNormalizer.normalize(null)).isEmpty();
        assertThat(InvoiceNumberNormalizer.normalize("")).isEmpty();
        assertThat(InvoiceNumberNormalizer.normalize("   ")).isEmpty();
    }

    @Test
    void distinctNumbersRemainDistinct() {
        assertThat(InvoiceNumberNormalizer.normalize("INV-1001"))
                .isNotEqualTo(InvoiceNumberNormalizer.normalize("INV-1002"));
    }
}

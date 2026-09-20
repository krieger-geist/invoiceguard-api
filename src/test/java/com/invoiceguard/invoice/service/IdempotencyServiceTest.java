package com.invoiceguard.invoice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceguard.exception.IdempotencyConflictException;
import com.invoiceguard.invoice.entity.IdempotencyRecord;
import com.invoiceguard.invoice.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyRecordRepository repository;

    private IdempotencyService service;

    private record SampleRequest(String invoiceNumber, String amount) {}

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(repository, new ObjectMapper());
    }

    @Test
    void sameRequestBodyProducesTheSameHash() {
        String hashA = service.hashRequest(new SampleRequest("INV-1", "100.00"));
        String hashB = service.hashRequest(new SampleRequest("INV-1", "100.00"));

        assertThat(hashA).isEqualTo(hashB);
    }

    @Test
    void differentRequestBodiesProduceDifferentHashes() {
        String hashA = service.hashRequest(new SampleRequest("INV-1", "100.00"));
        String hashB = service.hashRequest(new SampleRequest("INV-1", "200.00"));

        assertThat(hashA).isNotEqualTo(hashB);
    }

    @Test
    void assertMatchesPassesWhenHashesAreIdentical() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey("key-1");
        record.setRequestHash("abc123");

        assertThatCode(() -> service.assertMatches(record, "abc123")).doesNotThrowAnyException();
    }

    @Test
    void assertMatchesRejectsReuseWithDifferentBody() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey("key-1");
        record.setRequestHash("abc123");

        assertThatThrownBy(() -> service.assertMatches(record, "different-hash"))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("key-1");
    }
}

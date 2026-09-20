package com.invoiceguard.integration.webhook;

import com.invoiceguard.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * An organisation's registered webhook endpoint. {@code eventTypes} is a
 * comma-separated list of the dot-namespaced event names from the spec
 * (e.g. {@code invoice.approved}) — stored as one column rather than a join
 * table since a handful of event types per subscription is the expected
 * shape and a join table would only add query overhead for no real benefit.
 *
 * <p>{@code signingSecret} is stored in plain text. Unlike a password or
 * API key, the server must be able to read this value back on every
 * delivery to compute the HMAC signature — it can't be a one-way hash. A
 * production deployment should encrypt this column at rest the same way
 * {@code VendorBankAccount.encryptedAccountNumber} is (AES-GCM via a
 * dedicated key), which was intentionally left out here to avoid growing
 * the encryption-key surface area for a field that isn't itself a
 * financial credential.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription extends OrganizationScopedEntity {

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "event_types", nullable = false, length = 500)
    private String eventTypes;

    @Column(name = "signing_secret", nullable = false, length = 100)
    private String signingSecret;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public boolean subscribesTo(String eventType) {
        for (String type : eventTypes.split(",")) {
            if (type.trim().equalsIgnoreCase(eventType)) {
                return true;
            }
        }
        return false;
    }
}

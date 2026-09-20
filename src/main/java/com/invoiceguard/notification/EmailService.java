package com.invoiceguard.notification;

/**
 * Abstraction over outbound email so the auth module (and later the alert
 * module) doesn't depend on a concrete mail provider. The full notification
 * module (templates, retries, delivery tracking) is built out in a later
 * phase; for now a single logging implementation satisfies every caller.
 */
public interface EmailService {

    void sendEmailVerification(String toEmail, String recipientName, String verificationToken);

    void sendPasswordReset(String toEmail, String recipientName, String resetToken);
}

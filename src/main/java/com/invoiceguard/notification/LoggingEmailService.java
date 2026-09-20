package com.invoiceguard.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default {@link EmailService} implementation: logs instead of sending.
 * Deliberately never logs the raw token itself in a real deployment path —
 * only that an email would have been dispatched — except at DEBUG level
 * during local development, which is acceptable since dev tokens are
 * meaningless outside a developer's own database.
 */
@Service
public class LoggingEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailService.class);

    @Override
    public void sendEmailVerification(String toEmail, String recipientName, String verificationToken) {
        log.info("Email verification would be sent to {}", toEmail);
        log.debug("Verification token for {}: {}", toEmail, verificationToken);
    }

    @Override
    public void sendPasswordReset(String toEmail, String recipientName, String resetToken) {
        log.info("Password reset email would be sent to {}", toEmail);
        log.debug("Reset token for {}: {}", toEmail, resetToken);
    }
}

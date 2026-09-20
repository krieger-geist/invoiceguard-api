package com.invoiceguard.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM encryption for sensitive fields at rest — currently used only
 * for {@code VendorBankAccount.encryptedAccountNumber}. GCM is an
 * authenticated mode (it detects tampering, not just decrypts), which
 * matters for financial data where a corrupted/tampered ciphertext silently
 * decrypting to a plausible-looking wrong account number would be dangerous.
 *
 * <p>Ciphertext format stored in the database: {@code base64(iv || ciphertext+tag)}.
 * The IV is regenerated per encryption call and prepended, so no IV is ever reused
 * with the same key — reuse is the one catastrophic mistake that breaks GCM's
 * security guarantees.
 */
@Component
public class BankAccountEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    public BankAccountEncryptionService(EncryptionProperties properties) {
        String configured = properties.bankAccountEncryptionKey();
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException(
                    "invoiceguard.security.bank-account-encryption-key (BANK_ACCOUNT_ENCRYPTION_KEY) must be "
                            + "configured as a Base64-encoded 32-byte key — refusing to start without it");
        }
        byte[] keyBytes = Base64.getDecoder().decode(configured);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "BANK_ACCOUNT_ENCRYPTION_KEY must decode to exactly 32 bytes (AES-256); got " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt bank account data", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt bank account data", e);
        }
    }

    /** e.g. "1234567890123456" -&gt; "************3456". Used for every API-facing display of an account number. */
    public String mask(String rawAccountNumber) {
        if (rawAccountNumber.length() <= 4) {
            return "*".repeat(rawAccountNumber.length());
        }
        String lastFour = rawAccountNumber.substring(rawAccountNumber.length() - 4);
        return "*".repeat(rawAccountNumber.length() - 4) + lastFour;
    }
}

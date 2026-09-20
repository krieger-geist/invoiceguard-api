package com.invoiceguard.auth.repository;

import com.invoiceguard.auth.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true, r.revokedAt = CURRENT_TIMESTAMP "
            + "where r.userId = :userId and r.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId);
}

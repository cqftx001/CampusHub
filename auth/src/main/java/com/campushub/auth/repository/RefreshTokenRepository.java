package com.campushub.auth.repository;

import com.campushub.auth.domain.RefreshToken;
import com.campushub.auth.domain.RefreshTokenStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from RefreshToken token
            where token.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Query("""
        select token.sessionId
        from RefreshToken token
        where token.tokenHash = :tokenHash
        """)
    Optional<UUID> findSessionIdByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token
            from RefreshToken token
            where token.sessionId = :sessionId
              and token.status = :status
            """)
    Optional<RefreshToken> findBySessionIdAndStatusForUpdate(
            @Param("sessionId") UUID sessionId,
            @Param("status") RefreshTokenStatus status
    );

    List<RefreshToken> findAllBySessionIdAndStatus(
            UUID sessionId,
            RefreshTokenStatus status
    );
}

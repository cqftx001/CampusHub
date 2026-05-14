package com.campushub.identity.impl.domain.repository;

import com.campushub.identity.impl.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 作废某个用户的所有令牌
     * @param userId
     */
    void deleteAllByUserId(UUID userId);
}

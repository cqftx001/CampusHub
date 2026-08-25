package com.campushub.auth.repository;

import com.campushub.auth.domain.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<AuthAccount> findByUsernameOrEmail(
            String username,
            String email
    );

    Optional<AuthAccount> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AuthAccount account where account.id = :accountId")
    Optional<AuthAccount> findByIdForUpdate(@Param("accountId") UUID accountId);
}

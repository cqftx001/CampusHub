package com.campushub.auth.repository;

import com.campushub.auth.domain.PasswordCredential;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select credential
            from PasswordCredential credential
            where credential.accountId = :accountId
            """)
    Optional<PasswordCredential> findByIdForUpdate(@Param("accountId") UUID accountId);
}

package com.campushub.auth.repository;

import com.campushub.auth.domain.LoginSession;
import com.campushub.auth.domain.LoginSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoginSessionRepository extends JpaRepository<LoginSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select session
            from LoginSession session
            where session.id = :sessionId
            """)
    Optional<LoginSession> findByIdForUpdate(
            @Param("sessionId") UUID sessionId
    );

    List<LoginSession> findAllByAccountIdAndStatus(
            UUID accountId,
            LoginSessionStatus status
    );
}

package com.campushub.auth.repository;

import com.campushub.auth.domain.AuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthAccountRepository extends JpaRepository<AuthAccount, UUID> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<AuthAccount> findByUsernameOrEmail(
            String username,
            String email
    );

}

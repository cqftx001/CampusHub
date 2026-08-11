package com.campushub.auth.repository;

import com.campushub.auth.domain.PasswordCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, UUID> {


}

package com.campushub.auth.repository;

import com.campushub.auth.domain.Role;
import com.campushub.auth.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(RoleCode code);
}

package com.campushub.auth.repository;

import com.campushub.auth.domain.AccountRole;
import com.campushub.auth.domain.AccountRoleId;
import com.campushub.auth.domain.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;
import java.util.UUID;

public interface AccountRoleRepository extends JpaRepository<AccountRole, AccountRoleId> {

    @Query("""
            select role.code
            from Role role
            where role.id in (
                select accountRole.id.roleId
                from AccountRole accountRole
                where accountRole.id.accountId = :accountId
            )
            """)
    Set<RoleCode> findRoleCodesByAccountId(
            @Param("accountId") UUID accountId
    );
}

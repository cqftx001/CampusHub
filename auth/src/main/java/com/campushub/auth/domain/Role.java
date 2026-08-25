package com.campushub.auth.domain;

import com.campushub.shared.base.BaseEntity;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(
        name = "roles",
        schema = "auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_auth_roles_code",
                        columnNames = "code"
                )
        }
)
public class Role extends BaseEntity{

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            updatable = false,
            length = 32
    )
    private RoleCode code;

    protected Role(){
    }

    public Role(RoleCode code){
        this.code = Objects.requireNonNull(code);
    }

    public RoleCode getCode() {
        return code;
    }
}

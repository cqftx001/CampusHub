package com.campushub.identity.api;

import com.campushub.identity.api.dto.RegisterRequest;
import com.campushub.identity.api.vo.UserView;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Tag(name = "Identity Module", description = "Identity 模块内部 API，供其他模块调用")
public interface IdentityModuleApi {

    Optional<UserView> findUserById(UUID userId);

    List<UserView> findUsersByIds(Set<UUID> userIds);

    boolean isUserActive(UUID userId);
}

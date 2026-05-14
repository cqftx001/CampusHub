package com.campushub.security.context;

import com.campushub.shared.enums.CommonErrorCode;
import com.campushub.shared.exception.UnauthorizedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils(){}

    /**
     * 获取当前用用户ID
     */
    public static UUID getCurrentUserId(){
        Authentication authentication = getAuthentication();
        return UUID.fromString(authentication.getName());
    }

    /**
     * 获取当前用户权限
     */
    public static String getCurrentUserRoles(){
        Authentication authentication = getAuthentication();
        return authentication.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    /**
     * 判断用户是否已经登陆
     */
    public static boolean isAuthenticated(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    // -- Helper --
    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException(CommonErrorCode.UNAUTHORIZED);
        }

        return authentication;
    }
}

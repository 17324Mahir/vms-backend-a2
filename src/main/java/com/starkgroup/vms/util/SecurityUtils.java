package com.starkgroup.vms.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Locale;

/**
 * Utility for retrieving the current authenticated user identity.
 * Fallbacks to the default test account for backwards-compatible test execution.
 */
public final class SecurityUtils {

    public static final String DEFAULT_USER_EMAIL = "test@starkgroup.test";

    private SecurityUtils() {
    }

    /**
     * Resolves the email address of the currently authenticated principal.
     *
     * @return Lowercase email of the authenticated user, or DEFAULT_USER_EMAIL if unauthenticated.
     */
    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            if (auth.getPrincipal() instanceof Jwt jwt) {
                String sub = jwt.getSubject();
                if (sub != null && !sub.trim().isEmpty()) {
                    return sub.trim().toLowerCase(Locale.ROOT);
                }
            }
            String name = auth.getName();
            if (name != null && !name.trim().isEmpty()) {
                return name.trim().toLowerCase(Locale.ROOT);
            }
        }
        return DEFAULT_USER_EMAIL;
    }
}

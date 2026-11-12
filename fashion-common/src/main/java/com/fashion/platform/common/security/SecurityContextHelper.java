package com.fashion.platform.common.security;

import java.util.Optional;

public final class SecurityContextHelper {

    private static final ThreadLocal<UserContext> CURRENT_USER = new ThreadLocal<>();

    private SecurityContextHelper() {}

    public static void setContext(UserContext userContext) {
        CURRENT_USER.set(userContext);
    }

    public static Optional<UserContext> getContext() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    public static void clearContext() {
        CURRENT_USER.remove();
    }
}


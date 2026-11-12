package com.fashion.platform.common.security;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class UserContext {
    UUID userId;
    String username;
    UUID legalEntityId;
    UUID storeId;
    @Builder.Default
    Set<String> roles = Collections.emptySet();
    @Builder.Default
    Set<String> permissions = Collections.emptySet();

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}


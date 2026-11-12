package com.fashion.platform.web.filter;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserContextFilter implements Filter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_LEGAL_ENTITY_ID = "X-Legal-Entity-Id";
    public static final String HEADER_STORE_ID = "X-Store-Id";
    public static final String HEADER_ROLES = "X-Roles";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            try {
                String userIdHeader = httpRequest.getHeader(HEADER_USER_ID);
                String username = httpRequest.getHeader(HEADER_USERNAME);
                String legalEntityIdHeader = httpRequest.getHeader(HEADER_LEGAL_ENTITY_ID);
                String storeIdHeader = httpRequest.getHeader(HEADER_STORE_ID);
                String rolesHeader = httpRequest.getHeader(HEADER_ROLES);

                UUID userId = parseUuid(userIdHeader);
                UUID legalEntityId = parseUuid(legalEntityIdHeader);
                UUID storeId = parseUuid(storeIdHeader);

                Set<String> roles = Collections.emptySet();
                if (StringUtils.hasText(rolesHeader)) {
                    roles = Arrays.stream(rolesHeader.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                }

                UserContext context = UserContext.builder()
                        .userId(userId)
                        .username(username != null ? username : "anonymous")
                        .legalEntityId(legalEntityId)
                        .storeId(storeId)
                        .roles(roles)
                        .build();

                SecurityContextHelper.setContext(context);
                chain.doFilter(request, response);
            } finally {
                SecurityContextHelper.clearContext();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private UUID parseUuid(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


package com.fashion.platform.web;

import com.fashion.platform.web.filter.UserContextFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should return health status UP")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("fashion-platform")))
                .andExpect(jsonPath("$.sprint", is("Sprint 1 - Foundation")));
    }

    @Test
    @DisplayName("Should parse headers into UserContext correctly")
    void testUserContextFilter() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID legalEntityId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/system/context")
                        .header(UserContextFilter.HEADER_USER_ID, userId.toString())
                        .header(UserContextFilter.HEADER_USERNAME, "john_accountant")
                        .header(UserContextFilter.HEADER_LEGAL_ENTITY_ID, legalEntityId.toString())
                        .header(UserContextFilter.HEADER_ROLES, "ROLE_ACCOUNTANT,ROLE_FINANCE_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.username", is("john_accountant")))
                .andExpect(jsonPath("$.legalEntityId", is(legalEntityId.toString())))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("ROLE_ACCOUNTANT", "ROLE_FINANCE_MANAGER")));
    }
}


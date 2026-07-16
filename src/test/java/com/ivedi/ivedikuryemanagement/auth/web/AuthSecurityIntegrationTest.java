package com.ivedi.ivedikuryemanagement.auth.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/auth/me without authentication is rejected")
    void getCurrentUser_withoutAuthentication_isRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("CORS preflight allows configured localhost origin with Authorization header")
    void cors_allowedOrigin_preflightSucceeds() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS preflight rejects unconfigured origin and never returns wildcard with credentials")
    void cors_disallowedOrigin_doesNotReturnAllowOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register is disabled")
    void register_publicRegistrationDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("""
                                {"fullName":"Courier","email":"courier@example.com","password":"Password123","role":"COURIER"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PUBLIC_REGISTRATION_DISABLED"));
    }
}

package com.github.insight.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "test-login.enabled=true",
    "test-login.github-id=test-login-user",
    "test-login.email=test-login-user@example.local",
    "test-login.display-name=Test Login User"
})
@DisplayName("AuthController 테스트")
class AuthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("시험용 로그인 - OAuth 없이 세션 생성")
    void testLoginWithoutOAuthCreatesSession() throws Exception {
        String sessionId = mockMvc.perform(get("/auth/login"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "/?login=test"))
            .andExpect(cookie().exists("SESSION_ID"))
            .andReturn()
            .getResponse()
            .getCookie("SESSION_ID")
            .getValue();

        mockMvc.perform(get("/auth/me").cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedIn").value(true))
            .andExpect(jsonPath("$.githubId").value("test-login-user"))
            .andExpect(jsonPath("$.displayName").value("Test Login User"));
    }

    @Test
    @DisplayName("OAuth 설정 조회 - 시험용 로그인 상태 포함")
    void testConfigIncludesTestLoginFlag() throws Exception {
        mockMvc.perform(get("/auth/config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.configured").value(false))
            .andExpect(jsonPath("$.testLoginEnabled").value(true));
    }
}

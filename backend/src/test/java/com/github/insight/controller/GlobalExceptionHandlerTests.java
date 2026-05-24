package com.github.insight.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTests.TestController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("예외 처리 - 잘못된 요청")
    void testHandleBadRequest() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/not-a-number").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    @DisplayName("에러 응답 형식")
    void testErrorResponseFormat() throws Exception {
        mockMvc.perform(get("/test/runtime").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.path").value("/test/runtime"));
    }

    @RestController
    static class TestController {
        @GetMapping("/test/illegal-arg")
        String illegalArg() {
            throw new IllegalArgumentException("invalid input");
        }

        @GetMapping("/test/type-mismatch/{id}")
        String typeMismatch(@PathVariable Integer id) {
            return String.valueOf(id);
        }

        @GetMapping("/test/runtime")
        String runtimeError() {
            throw new RuntimeException("boom");
        }
    }
}

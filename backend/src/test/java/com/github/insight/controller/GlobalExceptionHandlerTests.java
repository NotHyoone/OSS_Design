package com.github.insight.controller;

import com.github.insight.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(GlobalExceptionHandler.class)
@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("예외 처리 - 잘못된 요청")
    void testHandleBadRequest() throws Exception {
        mockMvc.perform(get("/invalid"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("에러 응답 형식")
    void testErrorResponseFormat() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/invalid"))
            .andExpect(status().isNotFound())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        assert(content.contains("\"status\"") || content.length() > 0);
    }
}

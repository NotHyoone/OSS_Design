package com.github.insight.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("URL 안전성 테스트")
class UrlSafetyTests {

    @Test
    @DisplayName("githubId URL 인코딩 - 일반 ID")
    void testUrlEncoding_NormalId() {
        String githubId = "octocat";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertEquals("octocat", encoded);
    }

    @Test
    @DisplayName("githubId URL 인코딩 - 특수 문자")
    void testUrlEncoding_SpecialChars() {
        String githubId = "user/test";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertTrue(encoded.contains("%"));
        assertNotEquals(githubId, encoded);
    }

    @Test
    @DisplayName("githubId URL 인코딩 - 경로 탈출 시도")
    void testUrlEncoding_PathEscape() {
        String githubId = "../admin";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertNotEquals(githubId, encoded);
        assertTrue(encoded.contains("%2E"));
    }

    @Test
    @DisplayName("githubId URL 인코딩 - URL 삽입 시도")
    void testUrlEncoding_UrlInjection() {
        String githubId = "user?token=secret";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertNotEquals(githubId, encoded);
        assertTrue(encoded.contains("%3F"));
    }

    @Test
    @DisplayName("githubId URL 인코딩 - 공백")
    void testUrlEncoding_Spaces() {
        String githubId = "user name";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertNotEquals(githubId, encoded);
        assertTrue(encoded.contains("%20") || encoded.contains("+"));
    }

    @Test
    @DisplayName("repoName URL 인코딩 - 대시와 언더스코어")
    void testUrlEncoding_DashesAndUnderscores() {
        String repoName = "my-repo_name";
        String encoded = URLEncoder.encode(repoName, StandardCharsets.UTF_8);
        assertEquals(repoName, encoded);
    }

    @Test
    @DisplayName("repoName URL 인코딩 - 점")
    void testUrlEncoding_Dots() {
        String repoName = "my.repo.js";
        String encoded = URLEncoder.encode(repoName, StandardCharsets.UTF_8);
        assertEquals(repoName, encoded);
    }

    @Test
    @DisplayName("URL 인코딩 - 한글")
    void testUrlEncoding_Korean() {
        String githubId = "사용자";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertNotEquals(githubId, encoded);
        assertTrue(encoded.contains("%"));
    }

    @Test
    @DisplayName("URL 인코딩 안정성 - null은 처리하지 않음")
    void testUrlEncoding_SafeFromNull() {
        String githubId = "test";
        String encoded = URLEncoder.encode(githubId, StandardCharsets.UTF_8);
        assertNotNull(encoded);
    }
}

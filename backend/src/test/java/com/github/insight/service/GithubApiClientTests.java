package com.github.insight.service;

import com.github.insight.model.RepositoryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("GithubApiClient 테스트")
class GithubApiClientTests {

    @MockBean
    private RestTemplate restTemplate;

    private GithubApiClient githubApiClient;

    @BeforeEach
    void setUp() {
        githubApiClient = new GithubApiClient();
    }

    @Test
    @DisplayName("사용자 검증 - 존재하는 사용자")
    void testValidateUserExists_Success() {
        String userId = "testuser";
        when(restTemplate.exchange(
                contains("/users/" + userId),
                any(),
                any(),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(new HashMap<>()));

        assertTrue(githubApiClient.validateUserExists(userId));
    }

    @Test
    @DisplayName("사용자 검증 - 존재하지 않는 사용자")
    void testValidateUserExists_NotFound() {
        String userId = "nonexistent";
        when(restTemplate.exchange(
                contains("/users/" + userId),
                any(),
                any(),
                eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertFalse(githubApiClient.validateUserExists(userId));
    }

    @Test
    @DisplayName("Rate Limit 상태 조회")
    void testGetRateLimitStatus() {
        int status = githubApiClient.getRateLimitStatus();
        assertTrue(status >= 0);
    }

    @Test
    @DisplayName("Rate Limit 체크 - 여유 있을 때")
    void testCanMakeRequest_HasQuota() {
        assertTrue(githubApiClient.canMakeRequest());
    }

    @Test
    @DisplayName("저장소 조회 실패 시 빈 리스트 반환")
    void testGetRepositories_Error() {
        String userId = "testuser";
        when(restTemplate.exchange(
                contains("/users/" + userId + "/repos"),
                any(),
                any(),
                eq(List.class)))
                .thenThrow(new RuntimeException("API Error"));

        List<RepositoryData> repos = githubApiClient.getRepositories(userId);
        assertTrue(repos.isEmpty());
    }

    @Test
    @DisplayName("저장소 조회 성공")
    void testGetRepositories_Success() {
        String userId = "testuser";
        Map<String, Object> repoData = new HashMap<>();
        repoData.put("full_name", userId + "/test-repo");
        repoData.put("name", "test-repo");
        repoData.put("language", "Java");
        repoData.put("stargazers_count", 5);
        repoData.put("fork", false);
        repoData.put("pushed_at", Instant.now().toString());

        when(restTemplate.exchange(
                contains("/users/" + userId + "/repos"),
                any(),
                any(),
                eq(List.class)))
                .thenReturn(ResponseEntity.ok(List.of(repoData)));

        List<RepositoryData> repos = githubApiClient.getRepositories(userId);
        assertEquals(1, repos.size());
        assertEquals("test-repo", repos.get(0).getName());
    }

    @Test
    @DisplayName("커밋 조회 실패 시 빈 리스트 반환")
    void testGetCommits_Error() {
        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(List.class)))
                .thenThrow(new RuntimeException("API Error"));

        List<?> commits = githubApiClient.getCommits("testuser", "test-repo");
        assertTrue(commits.isEmpty());
    }

    @Test
    @DisplayName("언어 정보 조회 성공")
    void testGetLanguages_Success() {
        String userId = "testuser";
        String repoName = "test-repo";
        Map<String, Object> langData = new HashMap<>();
        langData.put("Java", 1000);
        langData.put("Python", 500);

        when(restTemplate.exchange(
                contains("/repos/" + userId + "/" + repoName + "/languages"),
                any(),
                any(),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(langData));

        Map<String, Long> languages = githubApiClient.getLanguages(userId, repoName);
        assertEquals(2, languages.size());
        assertEquals(1000L, languages.get("Java"));
    }

    @Test
    @DisplayName("PR 조회 - 빈 결과")
    void testGetPullRequests_Empty() {
        Map<String, Object> response = new HashMap<>();
        response.put("items", new ArrayList<>());

        when(restTemplate.exchange(
                contains("/search/issues"),
                any(),
                any(),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(response));

        List<?> prs = githubApiClient.getPullRequests("testuser");
        assertTrue(prs.isEmpty());
    }

    @Test
    @DisplayName("이슈 조회 - 빈 결과")
    void testGetIssues_Empty() {
        Map<String, Object> response = new HashMap<>();
        response.put("items", new ArrayList<>());

        when(restTemplate.exchange(
                contains("/search/issues"),
                any(),
                any(),
                eq(Map.class)))
                .thenReturn(ResponseEntity.ok(response));

        List<?> issues = githubApiClient.getIssues("testuser");
        assertTrue(issues.isEmpty());
    }
}

package com.github.insight.service;

import com.github.insight.dto.GithubRepo;
import com.github.insight.dto.GithubUserData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GitHub REST API 호출 전담 서비스.
 * <p>
 * 환경변수 GITHUB_TOKEN (또는 application.properties의 github.token)을 설정하면
 * 인증 요청으로 Rate Limit이 5000 req/h 로 상향됩니다.
 */
@Service
public class GithubApiService {

    private static final Logger log = LoggerFactory.getLogger(GithubApiService.class);
    private static final String API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${github.token:}")
    private String githubToken;

    /* ── 공통 헤더 ── */

    private HttpEntity<Void> requestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "GitHubActivityInsight/1.0");
        if (githubToken != null && !githubToken.isBlank()) {
            headers.set("Authorization", "Bearer " + githubToken);
        }
        return new HttpEntity<>(headers);
    }

    /* ── 사용자 조회 ── */

    /**
     * GitHub 사용자 존재 여부 확인.
     *
     * @param githubId GitHub 로그인 ID
     * @return 사용자 정보 Optional (없으면 empty)
     */
    public Optional<GithubUserData> getUser(String githubId) {
        try {
            ResponseEntity<GithubUserData> resp = restTemplate.exchange(
                    API_BASE + "/users/" + githubId,
                    HttpMethod.GET,
                    requestEntity(),
                    GithubUserData.class
            );
            return Optional.ofNullable(resp.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.warn("GitHub 사용자 조회 실패 ({}): {}", githubId, e.getMessage());
            throw e;
        }
    }

    /* ── 저장소 목록 ── */

    /**
     * 사용자의 공개 저장소 목록 조회 (최대 100개, 최근 업데이트 순).
     */
    public List<GithubRepo> getRepos(String githubId) {
        try {
            ResponseEntity<List<GithubRepo>> resp = restTemplate.exchange(
                    API_BASE + "/users/" + githubId + "/repos?per_page=100&sort=updated&type=public",
                    HttpMethod.GET,
                    requestEntity(),
                    new ParameterizedTypeReference<>() {}
            );
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("저장소 목록 조회 실패 ({}): {}", githubId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /* ── 이벤트 목록 ── */

    /**
     * 사용자의 최근 공개 이벤트 목록 조회 (최대 100건, 약 90일치).
     * 반환 타입은 Map 리스트로 처리하여 이벤트 타입별 payload를 유연하게 접근.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getEvents(String githubId) {
        try {
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    API_BASE + "/users/" + githubId + "/events/public?per_page=100",
                    HttpMethod.GET,
                    requestEntity(),
                    new ParameterizedTypeReference<>() {}
            );
            return resp.getBody() != null ? resp.getBody() : Collections.emptyList();
        } catch (Exception e) {
            log.warn("이벤트 목록 조회 실패 ({}): {}", githubId, e.getMessage());
            return Collections.emptyList();
        }
    }
}

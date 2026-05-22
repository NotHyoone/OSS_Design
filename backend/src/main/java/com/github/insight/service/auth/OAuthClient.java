package com.github.insight.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class OAuthClient {

    private static final Logger log = LoggerFactory.getLogger(OAuthClient.class);

    @Value("${github.oauth.client-id:}")
    private String clientId;

    @Value("${github.oauth.client-secret:}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri:http://localhost:8080/auth/callback}")
    private String redirectUri;

    private static final String AUTHORIZATION_URL = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token";
    private static final String API_BASE = "https://api.github.com";

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAuthorizationUrl(String state) {
        return AUTHORIZATION_URL
            + "?client_id=" + clientId
            + "&redirect_uri=" + redirectUri
            + "&scope=read:user,user:email"
            + "&state=" + state;
    }

    public String exchangeCodeForToken(String code) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("GitHub OAuth App이 설정되지 않았습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(TOKEN_URL, request, Map.class);
            Map<String, Object> body = resp.getBody();
            if (body == null || body.containsKey("error")) {
                String err = body != null ? (String) body.get("error_description") : "unknown";
                throw new RuntimeException("토큰 발급 실패: " + err);
            }
            return (String) body.get("access_token");
        } catch (Exception e) {
            log.error("OAuth 토큰 교환 실패: {}", e.getMessage());
            throw new RuntimeException("OAuth 인증 실패: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                API_BASE + "/user", HttpMethod.GET, entity, Map.class);
            return resp.getBody();
        } catch (Exception e) {
            log.error("GitHub 프로필 조회 실패: {}", e.getMessage());
            throw new RuntimeException("프로필 조회 실패: " + e.getMessage(), e);
        }
    }

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank();
    }
}

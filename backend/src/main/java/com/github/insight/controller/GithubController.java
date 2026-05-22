package com.github.insight.controller;

import com.github.insight.dto.ValidateResponse;
import com.github.insight.service.GithubApiClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

/**
 * GitHub 사용자 관련 API
 * GET /api/github/validate?id={githubId}
 */
@RestController
@RequestMapping("/api/github")
public class GithubController {

    private static final Pattern VALID_ID = Pattern.compile(
            "^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,37}[a-zA-Z0-9])?$"
    );

    private final GithubApiClient githubApiClient;

    public GithubController(GithubApiClient githubApiClient) {
        this.githubApiClient = githubApiClient;
    }

    /** GitHub ID 유효성 + 실존 여부 확인 (UC-01 Step 3) */
    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestParam String id) {
        if (id == null || id.isBlank() || id.length() > 39 || !VALID_ID.matcher(id).matches()) {
            return ResponseEntity.badRequest()
                .body(new ValidateResponse(false, null, "유효하지 않은 GitHub ID 형식입니다."));
        }
        try {
            boolean exists = githubApiClient.validateUserExists(id);
            if (exists) {
                com.github.insight.model.ActivityData preview = new com.github.insight.model.ActivityData();
                java.util.List<com.github.insight.model.RepositoryData> repos =
                    githubApiClient.getRepositories(id);
                String avatarUrl = repos.isEmpty() ? null :
                    "https://avatars.githubusercontent.com/" + id;
                return ResponseEntity.ok(new ValidateResponse(true, avatarUrl, null));
            } else {
                return ResponseEntity.ok(
                    new ValidateResponse(false, null, "GitHub에서 해당 ID를 찾을 수 없습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(502)
                .body(new ValidateResponse(false, null,
                    "GitHub API 호출 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."));
        }
    }
}

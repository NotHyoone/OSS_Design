package com.github.insight.controller;

import com.github.insight.model.User;
import com.github.insight.service.auth.AuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authService;

    @Value("${github.oauth.cookie-secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthenticationService authService) {
        this.authService = authService;
    }

    /** GitHub OAuth 로그인 시작 */
    @GetMapping("/login")
    public ResponseEntity<?> login(HttpServletResponse response) {
        if (!authService.isOAuthConfigured()) {
            return ResponseEntity.ok(Map.of(
                "configured", false,
                "message", "GitHub OAuth App이 설정되지 않았습니다. " +
                    "GITHUB_OAUTH_CLIENT_ID와 GITHUB_OAUTH_CLIENT_SECRET 환경변수를 설정하세요."
            ));
        }
        try {
            String redirectUrl = authService.initiateOAuthFlow();
            response.setHeader("Location", redirectUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return ResponseEntity.status(302).header("Location", redirectUrl).build();
        } catch (IllegalStateException e) {
            log.warn("로그인 요청 과부하: {}", e.getMessage());
            return ResponseEntity.status(429).body(Map.of("error", e.getMessage()));
        }
    }

    /** GitHub OAuth 콜백 처리 */
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) {
        try {
            User user = authService.handleOAuthCallback(code, state);

            Cookie sessionCookie = new Cookie("SESSION_ID", user.getSessionId());
            sessionCookie.setHttpOnly(true);
              sessionCookie.setSecure(cookieSecure);
            sessionCookie.setPath("/");
            sessionCookie.setMaxAge(1800); // 30분
            response.addCookie(sessionCookie);

            response.setHeader("Location", "/?login=success");
            response.setStatus(HttpServletResponse.SC_FOUND);
            return ResponseEntity.status(302).header("Location", "/?login=success").build();

        } catch (Exception e) {
            log.error("OAuth 콜백 처리 실패: {}", e.getMessage());
            String redirectUrl = "/?login=error&message=" +
                java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            response.setHeader("Location", redirectUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
        }
    }

    /** 현재 로그인 상태 조회 */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        if (sessionId == null) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
        Optional<User> userOpt = authService.getUserBySession(sessionId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "loggedIn",    true,
            "userId",      user.getUserId(),
            "githubId",    user.getGithubId(),
            "displayName", user.getDisplayName(),
            "avatarUrl",   user.getAvatarUrl()
        ));
    }

    /** OAuth 설정 여부 조회 */
    @GetMapping("/config")
    public ResponseEntity<?> getOAuthConfig() {
        return ResponseEntity.ok(Map.of(
            "configured", authService.isOAuthConfigured()
        ));
    }

    /** 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @CookieValue(value = "SESSION_ID", required = false) String sessionId,
            HttpServletResponse response) {
        if (sessionId != null) {
            authService.invalidateSession(sessionId);
        }
        Cookie expiredCookie = new Cookie("SESSION_ID", "");
        expiredCookie.setHttpOnly(true);
          expiredCookie.setSecure(cookieSecure);
        expiredCookie.setPath("/");
        expiredCookie.setMaxAge(0);
        response.addCookie(expiredCookie);
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }
}

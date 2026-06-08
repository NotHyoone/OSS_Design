package com.github.insight.service.auth;

import com.github.insight.entity.UserEntity;
import com.github.insight.model.User;
import com.github.insight.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final OAuthClient oauthClient;
    private final UserRepository userRepository;

    @Value("${github.oauth.session-timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    @Value("${test-login.enabled:false}")
    private boolean testLoginEnabled;

    @Value("${test-login.github-id:test-user}")
    private String testLoginGithubId;

    @Value("${test-login.email:test-user@example.local}")
    private String testLoginEmail;

    @Value("${test-login.display-name:Test User}")
    private String testLoginDisplayName;

    @Value("${test-login.avatar-url:}")
    private String testLoginAvatarUrl;

    /** state → timestamp (CSRF 방어) */
    private final Map<String, LocalDateTime> pendingStates = new ConcurrentHashMap<>();

    /** 동시 허용 최대 pending state 수 (봇/반복 요청 방어) */
    private static final int MAX_PENDING_STATES = 500;

    /** state 만료 시간(분) */
    private static final int STATE_EXPIRY_MINUTES = 10;

    public AuthenticationService(OAuthClient oauthClient, UserRepository userRepository) {
        this.oauthClient = oauthClient;
        this.userRepository = userRepository;
    }

    public String initiateOAuthFlow() {
        purgeExpiredStates();
        if (pendingStates.size() >= MAX_PENDING_STATES) {
            throw new IllegalStateException("현재 너무 많은 로그인 요청이 진행 중입니다. 잠시 후 다시 시도하세요.");
        }
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, LocalDateTime.now());
        return oauthClient.getAuthorizationUrl(state);
    }

    /** 만료된 pending state를 일괄 제거한다. */
    private void purgeExpiredStates() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(STATE_EXPIRY_MINUTES);
        pendingStates.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
    }

    public User handleOAuthCallback(String code, String state) {
        if (!validateState(state)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 OAuth state입니다.");
        }
        pendingStates.remove(state);

        String accessToken = oauthClient.exchangeCodeForToken(code);
        Map<String, Object> profile = oauthClient.getUserProfile(accessToken);

        String githubId   = String.valueOf(profile.getOrDefault("login", ""));
        String email      = (String) profile.getOrDefault("email", "");
        String displayName = (String) profile.getOrDefault("name", githubId);
        String avatarUrl  = (String) profile.getOrDefault("avatar_url", "");

        UserEntity entity = userRepository.findByGithubId(githubId)
            .orElseGet(UserEntity::new);
        if (entity.getGithubId() == null || entity.getGithubId().isBlank()) {
            entity.setGithubId(githubId);
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setEmail(email != null ? email : "");
        entity.setDisplayName(displayName);
        entity.setAvatarUrl(avatarUrl);
        entity.updateLastLogin();

        String sessionId = generateSessionId();
        entity.setSessionId(sessionId);
        userRepository.save(entity);

        User user = toModel(entity);

        log.info("OAuth 로그인 성공: {}", githubId);
        return user;
    }

    public boolean validateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        Optional<UserEntity> entityOpt = userRepository.findBySessionId(sessionId);
        if (entityOpt.isEmpty()) return false;

        UserEntity entity = entityOpt.get();
        LocalDateTime lastActivity = entity.getLastLoginAt();
        if (lastActivity == null) {
            invalidateSession(sessionId);
            return false;
        }
        if (lastActivity.plusMinutes(sessionTimeoutMinutes).isBefore(LocalDateTime.now())) {
            invalidateSession(sessionId);
            return false;
        }

        entity.updateLastLogin();
        userRepository.save(entity);
        return true;
    }

    public void invalidateSession(String sessionId) {
        userRepository.findBySessionId(sessionId).ifPresent(entity -> {
            entity.setSessionId(null);
            userRepository.save(entity);
        });
    }

    public String createSession(User user) {
        if (user == null || user.getGithubId() == null || user.getGithubId().isBlank()) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }

        String sessionId = generateSessionId();
        UserEntity entity = userRepository.findByGithubId(user.getGithubId())
            .orElseGet(UserEntity::new);
        if (entity.getGithubId() == null || entity.getGithubId().isBlank()) {
            entity.setGithubId(user.getGithubId());
            entity.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now());
            entity.setEmail(user.getEmail() != null ? user.getEmail() : "");
        }
        entity.setDisplayName(user.getDisplayName());
        entity.setAvatarUrl(user.getAvatarUrl());
        entity.setSessionId(sessionId);
        entity.updateLastLogin();
        userRepository.save(entity);
        return sessionId;
    }

    public User createTestLoginSession() {
        if (!testLoginEnabled) {
            throw new IllegalStateException("시험용 로그인이 비활성화되어 있습니다.");
        }

        User user = new User(
            testLoginGithubId,
            testLoginEmail,
            testLoginDisplayName,
            testLoginAvatarUrl
        );
        String sessionId = createSession(user);
        return userRepository.findBySessionId(sessionId)
            .map(this::toModel)
            .orElseThrow(() -> new IllegalStateException("시험용 세션 생성에 실패했습니다."));
    }

    public Optional<User> getUserBySession(String sessionId) {
        if (!validateSession(sessionId)) return Optional.empty();
        return userRepository.findBySessionId(sessionId).map(this::toModel);
    }

    public boolean isOAuthConfigured() {
        return oauthClient.isConfigured();
    }

    public boolean isTestLoginEnabled() {
        return testLoginEnabled;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private boolean validateState(String state) {
        LocalDateTime created = pendingStates.get(state);
        if (created == null) return false;
        return created.plusMinutes(STATE_EXPIRY_MINUTES).isAfter(LocalDateTime.now());
    }

    /**
     * 1시간마다 만료된 세션을 정리한다.
     * - users.sessionId: sessionTimeoutMinutes 초과 항목 제거
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpiredSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(sessionTimeoutMinutes);
        int removedCount = 0;
        for (UserEntity entity : userRepository.findBySessionIdIsNotNullAndLastLoginAtBefore(cutoff)) {
            entity.setSessionId(null);
            userRepository.save(entity);
            removedCount++;
        }

        if (removedCount > 0) {
            log.info("만료된 세션 정리 완료: {}건 제거", removedCount);
        }
    }

    private User toModel(UserEntity entity) {
        User user = new User();
        user.setUserId(entity.getUserId());
        user.setGithubId(entity.getGithubId());
        user.setEmail(entity.getEmail());
        user.setDisplayName(entity.getDisplayName());
        user.setAvatarUrl(entity.getAvatarUrl());
        user.setSessionId(entity.getSessionId());
        user.setCreatedAt(entity.getCreatedAt());
        user.setLastLoginAt(entity.getLastLoginAt());
        return user;
    }
}

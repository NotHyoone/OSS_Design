package com.github.insight.service.auth;

import com.github.insight.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${github.oauth.session-timeout-minutes:30}")
    private int sessionTimeoutMinutes;

    /** sessionId → User */
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    /** sessionId → lastActivityTime */
    private final Map<String, LocalDateTime> sessionActivity = new ConcurrentHashMap<>();

    /** githubId → User (user store) */
    private final Map<String, User> users = new ConcurrentHashMap<>();

    /** state → timestamp (CSRF 방어) */
    private final Map<String, LocalDateTime> pendingStates = new ConcurrentHashMap<>();

    /** 동시 허용 최대 pending state 수 (봇/반복 요청 방어) */
    private static final int MAX_PENDING_STATES = 500;

    /** state 만료 시간(분) */
    private static final int STATE_EXPIRY_MINUTES = 10;

    public AuthenticationService(OAuthClient oauthClient) {
        this.oauthClient = oauthClient;
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

        User user = users.computeIfAbsent(githubId,
            k -> new User(githubId, email != null ? email : "", displayName, avatarUrl));
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.updateLastLogin();

        String sessionId = createSession(user);
        user.setSessionId(sessionId);
        users.put(githubId, user);

        log.info("OAuth 로그인 성공: {}", githubId);
        return user;
    }

    public boolean validateSession(String sessionId) {
        if (sessionId == null || !sessions.containsKey(sessionId)) return false;
        LocalDateTime lastActivity = sessionActivity.get(sessionId);
        if (lastActivity == null) return false;
        if (lastActivity.plusMinutes(sessionTimeoutMinutes).isBefore(LocalDateTime.now())) {
            invalidateSession(sessionId);
            return false;
        }
        sessionActivity.put(sessionId, LocalDateTime.now());
        return true;
    }

    public void invalidateSession(String sessionId) {
        User user = sessions.remove(sessionId);
        sessionActivity.remove(sessionId);
        if (user != null) {
            user.setSessionId(null);
        }
    }

    public String createSession(User user) {
        String sessionId = generateSessionId();
        sessions.put(sessionId, user);
        sessionActivity.put(sessionId, LocalDateTime.now());
        return sessionId;
    }

    public Optional<User> getUserBySession(String sessionId) {
        if (!validateSession(sessionId)) return Optional.empty();
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public boolean isOAuthConfigured() {
        return oauthClient.isConfigured();
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    private boolean validateState(String state) {
        LocalDateTime created = pendingStates.get(state);
        if (created == null) return false;
        return created.plusMinutes(STATE_EXPIRY_MINUTES).isAfter(LocalDateTime.now());
    }
}

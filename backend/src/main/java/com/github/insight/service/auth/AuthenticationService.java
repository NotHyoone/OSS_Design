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

    public AuthenticationService(OAuthClient oauthClient) {
        this.oauthClient = oauthClient;
    }

    public String initiateOAuthFlow() {
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, LocalDateTime.now());
        return oauthClient.getAuthorizationUrl(state);
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
        return created.plusMinutes(10).isAfter(LocalDateTime.now());
    }
}

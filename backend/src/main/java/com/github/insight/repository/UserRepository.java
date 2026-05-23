package com.github.insight.repository;

import com.github.insight.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByGithubId(String githubId);
    Optional<UserEntity> findBySessionId(String sessionId);
    Optional<UserEntity> findByEmail(String email);
}

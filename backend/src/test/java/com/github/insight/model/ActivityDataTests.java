package com.github.insight.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ActivityData 모델 테스트")
class ActivityDataTests {

    private ActivityData activityData;

    @BeforeEach
    void setUp() {
        activityData = new ActivityData("test-request-123");
    }

    @Test
    @DisplayName("초기 생성 확인")
    void testInitialization() {
        assertNotNull(activityData);
        assertEquals("test-request-123", activityData.getRequestId());
        assertTrue(activityData.getRepositories().isEmpty());
        assertTrue(activityData.getCommits().isEmpty());
        assertTrue(activityData.getLanguages().isEmpty());
    }

    @Test
    @DisplayName("저장소 추가")
    void testAddRepository() {
        RepositoryData repo = new RepositoryData();
        repo.setRepoId("test/repo");
        repo.setName("repo");

        activityData.addRepository(repo);

        assertEquals(1, activityData.getRepositories().size());
        assertEquals("repo", activityData.getRepositories().get(0).getName());
    }

    @Test
    @DisplayName("커밋 추가")
    void testAddCommit() {
        CommitData commit = new CommitData("sha1", "repo", LocalDateTime.now(), "message");

        activityData.addCommit(commit);

        assertEquals(1, activityData.getCommits().size());
        assertEquals("sha1", activityData.getCommits().get(0).getCommitId());
    }

    @Test
    @DisplayName("데이터 유효성 검증 - 유효함")
    void testValidate_Valid() {
        RepositoryData repo = new RepositoryData();
        repo.setRepoId("test/repo");
        activityData.addRepository(repo);

        CommitData commit = new CommitData("sha1", "repo", LocalDateTime.now(), "msg");
        activityData.addCommit(commit);

        assertTrue(activityData.validate());
    }

    @Test
    @DisplayName("데이터 유효성 검증 - 저장소 없음")
    void testValidate_NoRepository() {
        CommitData commit = new CommitData("sha1", "repo", LocalDateTime.now(), "msg");
        activityData.addCommit(commit);

        assertFalse(activityData.validate());
    }

    @Test
    @DisplayName("데이터 유효성 검증 - 커밋 없음")
    void testValidate_NoCommit() {
        RepositoryData repo = new RepositoryData();
        repo.setRepoId("test/repo");
        activityData.addRepository(repo);

        assertFalse(activityData.validate());
    }

    @Test
    @DisplayName("비어있음 확인 - 완전히 비어있음")
    void testIsEmpty_True() {
        assertTrue(activityData.isEmpty());
    }

    @Test
    @DisplayName("비어있음 확인 - 데이터 있음")
    void testIsEmpty_False() {
        RepositoryData repo = new RepositoryData();
        repo.setRepoId("test/repo");
        activityData.addRepository(repo);

        assertFalse(activityData.isEmpty());
    }

    @Test
    @DisplayName("충분한 데이터 확인")
    void testHasEnoughData() {
        RepositoryData repo = new RepositoryData();
        repo.setRepoId("test/repo");
        activityData.addRepository(repo);

        CommitData commit = new CommitData("sha1", "repo", LocalDateTime.now(), "msg");
        activityData.addCommit(commit);

        activityData.getLanguages().put("Java", 1000L);

        assertTrue(activityData.hasEnoughData());
    }

    @Test
    @DisplayName("중복 제거")
    void testDeduplicate() {
        CommitData commit1 = new CommitData("sha1", "repo", LocalDateTime.now(), "msg1");
        CommitData commit2 = new CommitData("sha1", "repo", LocalDateTime.now(), "msg2");
        CommitData commit3 = new CommitData("sha2", "repo", LocalDateTime.now(), "msg3");

        activityData.addCommit(commit1);
        activityData.addCommit(commit2);
        activityData.addCommit(commit3);

        activityData.deduplicate();

        assertEquals(2, activityData.getCommits().size());
    }

    @Test
    @DisplayName("PR 추가")
    void testAddPullRequest() {
        PullRequestData pr = new PullRequestData("pr-1", "repo", "closed",
            LocalDateTime.now(), LocalDateTime.now(), true);

        activityData.addPullRequest(pr);

        assertEquals(1, activityData.getPullRequests().size());
    }

    @Test
    @DisplayName("이슈 추가")
    void testAddIssue() {
        IssueData issue = new IssueData("issue-1", "repo", "closed",
            LocalDateTime.now(), LocalDateTime.now());

        activityData.addIssue(issue);

        assertEquals(1, activityData.getIssues().size());
    }

    @Test
    @DisplayName("언어 정보 설정")
    void testSetLanguages() {
        Map<String, Long> languages = new HashMap<>();
        languages.put("Java", 1000L);
        languages.put("Python", 500L);

        activityData.setLanguages(languages);

        assertEquals(2, activityData.getLanguages().size());
        assertEquals(1000L, activityData.getLanguages().get("Java"));
    }
}

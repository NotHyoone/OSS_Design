package com.github.insight.service;

import com.github.insight.model.*;
import com.github.insight.model.enums.TrustLevel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricCalculator {

    public Metrics calculate(ActivityData data) {
        Metrics metrics = new Metrics(data.getRequestId());

        float activity = calculateActivity(data);
        float diversity = calculateDiversity(data);
        float collaboration = calculateCollaboration(data);
        float persistence = calculatePersistence(data);

        metrics.setActivityScore(activity);
        metrics.setDiversityScore(diversity);
        metrics.setCollaborationScore(collaboration);
        metrics.setPersistenceScore(persistence);

        metrics.addDescription("activity", buildActivityDesc(data));
        metrics.addDescription("diversity", buildDiversityDesc(data));
        metrics.addDescription("collab", buildCollabDesc(data));
        metrics.addDescription("persist", buildPersistDesc(data));

        boolean anomalies = detectAnomalies(data, metrics);
        TrustLevel trust = deriveTrustLevel(data, anomalies);
        metrics.setTrustLevel(trust);
        metrics.setCalculatedAt(LocalDateTime.now());

        return metrics;
    }

    public float calculateActivity(ActivityData data) {
        LocalDateTime cutoff = LocalDateTime.now().minus(90, ChronoUnit.DAYS);

        List<CommitData> recent = data.getCommits().stream()
            .filter(c -> c.getCommittedAt() != null && c.getCommittedAt().isAfter(cutoff))
            .filter(c -> !c.isBot() && !c.isMerge())
            .collect(Collectors.toList());

        long totalCommits = recent.size();
        Set<String> activeDays = recent.stream()
            .map(c -> c.getCommittedAt().toLocalDate().toString())
            .collect(Collectors.toSet());

        int commitScore = (int) Math.min(100, totalCommits * 100 / 120L);
        int dayScore    = (int) Math.min(100, activeDays.size() * 100 / 60);
        return normalizeScore(commitScore * 0.7f + dayScore * 0.3f, 0, 100);
    }

    public float calculateDiversity(ActivityData data) {
        Set<String> langs = data.getRepositories().stream()
            .filter(r -> !r.isFork() && r.getLanguage() != null && !r.getLanguage().isBlank())
            .map(RepositoryData::getLanguage)
            .collect(Collectors.toSet());

        long count = Math.max(langs.size(), data.getLanguages().size());
        return switch ((int) Math.min(count, 8)) {
            case 0 -> 8;
            case 1 -> 25;
            case 2 -> 42;
            case 3 -> 58;
            case 4 -> 70;
            case 5 -> 80;
            case 6 -> 87;
            case 7 -> 92;
            default -> 96;
        };
    }

    public float calculateCollaboration(ActivityData data) {
        long collabCount = data.getPullRequests().size() + data.getIssues().size();
        long forkCount   = data.getRepositories().stream().filter(RepositoryData::isFork).count();
        long totalRepos  = data.getRepositories().size();
        double forkRatio = totalRepos > 0 ? (double) forkCount / totalRepos : 0.0;

        int eventScore = (int) Math.min(70, collabCount * 70 / 25.0);
        int forkScore  = (int) (forkRatio * 30);
        return Math.max(0, Math.min(100, eventScore + forkScore));
    }

    public float calculatePersistence(ActivityData data) {
        LocalDateTime cutoff = LocalDateTime.now().minus(365, ChronoUnit.DAYS);

        Set<String> activeMonths = data.getCommits().stream()
            .filter(c -> c.getCommittedAt() != null && c.getCommittedAt().isAfter(cutoff))
            .map(c -> c.getCommittedAt().getYear() + "-"
                + String.format("%02d", c.getCommittedAt().getMonthValue()))
            .collect(Collectors.toSet());

        data.getRepositories().stream()
            .filter(r -> r.getLastUpdatedAt() != null && r.getLastUpdatedAt().isAfter(cutoff))
            .map(r -> r.getLastUpdatedAt().getYear() + "-"
                + String.format("%02d", r.getLastUpdatedAt().getMonthValue()))
            .forEach(activeMonths::add);

        int months = activeMonths.size();
        return months >= 12 ? 100
             : months >= 10 ? 90
             : months >= 8  ? 78
             : months >= 6  ? 64
             : months >= 4  ? 50
             : months >= 2  ? 32
             : months == 1  ? 18
             : 5;
    }

    public boolean validateActivityData(ActivityData data) {
        return data != null && !data.isEmpty();
    }

    public float normalizeScore(float value, float min, float max) {
        if (max == min) return 0;
        return Math.max(0, Math.min(100, (value - min) / (max - min) * 100));
    }

    public List<Float> filterOutliers(List<Float> scores) {
        if (scores.size() < 3) return scores;
        double mean = scores.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        double variance = scores.stream()
            .mapToDouble(s -> Math.pow(s - mean, 2)).average().orElse(0);
        double sigma = Math.sqrt(variance);
        return scores.stream()
            .filter(s -> Math.abs(s - mean) <= 2 * sigma)
            .collect(Collectors.toList());
    }

    public boolean detectAnomalies(ActivityData data, Metrics target) {
        Map<String, Long> msgCount = new HashMap<>();
        for (CommitData c : data.getCommits()) {
            if (c.getMessage() != null && !c.getMessage().isBlank()) {
                msgCount.merge(c.getMessage(), 1L, Long::sum);
            }
        }
        boolean hasRepeat = msgCount.values().stream().anyMatch(v -> v >= 20);
        long botCount = data.getCommits().stream().filter(CommitData::isBot).count();
        boolean hasBots = botCount > 0;

        if (hasRepeat || hasBots) {
            String note = "";
            if (hasBots)    note += "bot-commits(" + botCount + ") detected; ";
            if (hasRepeat)  note += "repeated-message anomaly detected; ";
            target.setNotes(note.trim());
            return true;
        }
        return false;
    }

    private TrustLevel deriveTrustLevel(ActivityData data, boolean anomalies) {
        if (anomalies) return TrustLevel.LIMITED;
        int repos = data.getRepositories().size();
        int commits = data.getCommitCount();
        int months = data.getTimeRange();
        if (repos >= 3 && commits >= 10 && months >= 6) return TrustLevel.HIGH;
        if (repos >= 1 && commits >= 1)                 return TrustLevel.PARTIAL;
        return TrustLevel.LOW;
    }

    private String buildActivityDesc(ActivityData data) {
        LocalDateTime cutoff = LocalDateTime.now().minus(90, ChronoUnit.DAYS);
        long cnt = data.getCommits().stream()
            .filter(c -> c.getCommittedAt() != null && c.getCommittedAt().isAfter(cutoff))
            .filter(c -> !c.isBot() && !c.isMerge()).count();
        if (cnt >= 100) return String.format("90일간 커밋 %d회 – 매우 활발", cnt);
        if (cnt >= 50)  return String.format("90일간 커밋 %d회 – 활발", cnt);
        if (cnt >= 20)  return String.format("90일간 커밋 %d회 – 보통", cnt);
        if (cnt > 0)    return String.format("90일간 커밋 %d회 – 낮음", cnt);
        return "최근 90일 공개 커밋 없음";
    }

    private String buildDiversityDesc(ActivityData data) {
        Set<String> langs = data.getRepositories().stream()
            .filter(r -> !r.isFork() && r.getLanguage() != null)
            .map(RepositoryData::getLanguage).collect(Collectors.toSet());
        if (langs.isEmpty()) return "공개 저장소에서 언어 정보 없음";
        return String.format("%d개 언어 사용 (%s)", langs.size(), String.join(", ", langs));
    }

    private String buildCollabDesc(ActivityData data) {
        long cnt = data.getPullRequests().size() + data.getIssues().size();
        if (cnt >= 20) return String.format("PR·Issue 참여 %d건 – 활발한 협업", cnt);
        if (cnt >= 8)  return String.format("PR·Issue 참여 %d건 – 보통", cnt);
        if (cnt >= 2)  return String.format("PR·Issue 참여 %d건 – 초기 단계", cnt);
        return "PR·Issue 이벤트 거의 없음";
    }

    private String buildPersistDesc(ActivityData data) {
        LocalDateTime cutoff = LocalDateTime.now().minus(365, ChronoUnit.DAYS);
        Set<String> months = data.getCommits().stream()
            .filter(c -> c.getCommittedAt() != null && c.getCommittedAt().isAfter(cutoff))
            .map(c -> c.getCommittedAt().getYear() + "-"
                + String.format("%02d", c.getCommittedAt().getMonthValue()))
            .collect(Collectors.toSet());
        int m = months.size();
        if (m == 0) return "최근 1년 공개 활동 없음";
        return String.format("최근 1년 중 %d개월 활동", m);
    }
}

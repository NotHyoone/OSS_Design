package com.github.insight.service;

import com.github.insight.model.*;
import com.github.insight.model.enums.DeveloperType;
import com.github.insight.model.enums.TrustLevel;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportAssembler {

    public Map<String, Object> toViewModel(AnalysisResult result, Metrics metrics) {
        Map<String, Object> vm = new LinkedHashMap<>();
        vm.put("githubId",      result.getGithubId());
        vm.put("avatarUrl",     result.getAvatarUrl());
        vm.put("analysisDate",  result.getCreatedAt() != null
                                    ? result.getCreatedAt().toString() : "");
        vm.put("developerType", formatDeveloperType(result.getDeveloperType()));
        vm.put("totalScore",    result.getTotalScore());
        vm.put("trustLevel",    result.getTrustLevel() != null
                                    ? result.getTrustLevel().name() : "HIGH");
        vm.put("summaryText",   result.getSummary());
        vm.put("lowTrust",      result.hasLowTrust());

        if (metrics != null) {
            Map<String, Object> metricsMap = new LinkedHashMap<>();
            metricsMap.put("activity",  buildMetricScore(
                (int) metrics.getActivityScore(),
                metrics.getDescriptions().getOrDefault("activity", "")));
            metricsMap.put("diversity", buildMetricScore(
                (int) metrics.getDiversityScore(),
                metrics.getDescriptions().getOrDefault("diversity", "")));
            metricsMap.put("collab",    buildMetricScore(
                (int) metrics.getCollaborationScore(),
                metrics.getDescriptions().getOrDefault("collab", "")));
            metricsMap.put("persist",   buildMetricScore(
                (int) metrics.getPersistenceScore(),
                metrics.getDescriptions().getOrDefault("persist", "")));
            vm.put("metrics", metricsMap);
        }

        List<String> improvements = Optional.ofNullable(result.getImprovements())
            .orElse(Collections.emptyList())
            .stream()
            .map(FeedbackItem::getActionGuide)
            .collect(Collectors.toList());
        vm.put("strengths",    result.getStrengths() != null ? result.getStrengths() : List.of());
        vm.put("improvements", improvements);

        return vm;
    }

    public Map<String, Object> toPdfModel(AnalysisResult result, Metrics metrics) {
        Map<String, Object> pdf = toViewModel(result, metrics);
        pdf.put("ruleVersion", result.getRuleVersion());
        pdf.put("requestId",   result.getRequestId());
        return pdf;
    }

    public Map<String, Object> toCompareModel(List<AnalysisResult> results) {
        List<Map<String, Object>> entries = new ArrayList<>();
        for (AnalysisResult r : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("score",        r.getTotalScore());
            entry.put("developerType", formatDeveloperType(r.getDeveloperType()));
            entry.put("date",         r.getCreatedAt() != null ? r.getCreatedAt().toString() : "");
            entry.put("requestId",    r.getRequestId());
            entries.add(entry);
        }
        Map<String, Object> compare = new LinkedHashMap<>();
        compare.put("entries", entries);
        compare.put("count",   entries.size());
        return compare;
    }

    private Map<String, Object> buildMetricScore(int score, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("score", score);
        m.put("desc",  desc);
        return m;
    }

    private String formatDeveloperType(DeveloperType type) {
        if (type == null) return "Beginner";
        return switch (type) {
            case ADVANCED -> "Advanced Developer";
            case JUNIOR   -> "Junior Developer";
            case BEGINNER -> "Beginner";
        };
    }
}

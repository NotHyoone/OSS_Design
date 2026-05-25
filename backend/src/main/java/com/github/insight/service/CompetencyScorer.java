package com.github.insight.service;

import com.github.insight.model.AnalysisResult;
import com.github.insight.model.Metrics;
import com.github.insight.model.enums.DeveloperType;
import com.github.insight.model.enums.TrustLevel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CompetencyScorer {

    private Map<String, Float> weights = Map.of(
        "activity",      0.25f,
        "diversity",     0.25f,
        "collaboration", 0.25f,
        "persistence",   0.25f
    );
    private final String ruleVersion = "1.0";
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    public AnalysisResult evaluate(Metrics metrics) {
        AnalysisResult result = new AnalysisResult();
        int total = score(metrics);
        result.setTotalScore(total);
        result.setDeveloperType(classifyType(total));
        result.setStrengths(identifyStrengths(metrics));
        result.setTrustLevel(metrics.getTrustLevel());
        result.setRuleVersion(ruleVersion);
        result.setImprovements(new ArrayList<>());
        return result;
    }

    public int score(Metrics metrics) {
        float raw = metrics.getActivityScore()      * weights.getOrDefault("activity", 0.25f)
                  + metrics.getDiversityScore()     * weights.getOrDefault("diversity", 0.25f)
                  + metrics.getCollaborationScore() * weights.getOrDefault("collaboration", 0.25f)
                  + metrics.getPersistenceScore()   * weights.getOrDefault("persistence", 0.25f);
        return Math.max(MIN_SCORE, Math.min(MAX_SCORE, Math.round(raw)));
    }

    public DeveloperType classifyType(int score) {
        if (score >= 70) return DeveloperType.ADVANCED;
        if (score >= 40) return DeveloperType.JUNIOR;
        return DeveloperType.BEGINNER;
    }

    public List<String> identifyStrengths(Metrics metrics) {
        List<String> strengths = new ArrayList<>();
        if (metrics.getActivityScore() >= 70)      strengths.add("activity");
        if (metrics.getDiversityScore() >= 70)     strengths.add("diversity");
        if (metrics.getCollaborationScore() >= 70) strengths.add("collaboration");
        if (metrics.getPersistenceScore() >= 70)   strengths.add("persistence");
        return strengths;
    }

    public List<String> identifyWeaknesses(Metrics metrics) {
        List<String> weaknesses = new ArrayList<>();
        if (metrics.getActivityScore() < 40)      weaknesses.add("activity");
        if (metrics.getDiversityScore() < 40)     weaknesses.add("diversity");
        if (metrics.getCollaborationScore() < 40) weaknesses.add("collaboration");
        if (metrics.getPersistenceScore() < 40)   weaknesses.add("persistence");
        return weaknesses;
    }

    public boolean isValidScore(int score) {
        return score >= MIN_SCORE && score <= MAX_SCORE;
    }

    public boolean validateWeightSum() {
        float sum = weights.values().stream().reduce(0f, Float::sum);
        return Math.abs(sum - 1.0f) < 0.001f;
    }

    public Map<String, Float> getAppliedWeights() {
        return weights;
    }

    public String buildSummaryText(String githubId, AnalysisResult result, Metrics metrics) {
        String best = List.of(
            Map.entry("활동성", metrics.getActivityScore()),
            Map.entry("기술 다양성", metrics.getDiversityScore()),
            Map.entry("협업도", metrics.getCollaborationScore()),
            Map.entry("지속성", metrics.getPersistenceScore())
        ).stream()
         .max(java.util.Comparator.comparingDouble(e -> e.getValue()))
         .map(Map.Entry::getKey)
         .orElse("전반적인 역량");

        String typeLabel = result.getDeveloperType() == DeveloperType.ADVANCED ? "고급 개발자"
                         : result.getDeveloperType() == DeveloperType.JUNIOR   ? "주니어 개발자"
                         : "입문 개발자";

        return String.format(
            "%s님은 종합 %d점으로 '%s' 수준으로 평가됩니다. "
            + "특히 %s 항목이 두드러지며, 꾸준한 활동을 유지하면 다음 단계로 성장할 수 있습니다.",
            githubId, result.getTotalScore(), typeLabel, best);
    }

    public TrustLevel determineTrustLevel(Metrics metrics) {
        return metrics.getTrustLevel();
    }
}

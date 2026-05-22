package com.github.insight.service;

import com.github.insight.model.FeedbackItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedbackGenerator {

    private static final Map<String, String[]> FEEDBACK_RULES = Map.of(
        "activity", new String[]{
            "최근 90일간 커밋 활동이 부족합니다.",
            "주 3회 이상 커밋하는 습관을 기르세요. 작은 작업도 커밋으로 남기면 활동성이 향상됩니다."
        },
        "diversity", new String[]{
            "사용 언어 및 기술 스택이 제한적입니다.",
            "새로운 언어나 프레임워크를 학습하고 소규모 프로젝트를 만들어 보세요."
        },
        "collaboration", new String[]{
            "PR·Issue 참여 등 협업 활동이 적습니다.",
            "오픈소스 프로젝트에 Issue를 남기거나 PR을 제출하는 것으로 협업 경험을 쌓으세요."
        },
        "persistence", new String[]{
            "장기적인 활동 지속성이 낮습니다.",
            "매월 최소 1개 이상의 커밋을 유지하는 것을 목표로 하세요. 꾸준함이 성장의 핵심입니다."
        }
    );

    public List<FeedbackItem> generate(List<String> weaknesses) {
        if (weaknesses == null || weaknesses.isEmpty()) return Collections.emptyList();

        return weaknesses.stream()
            .map(this::mapWeaknessToAction)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(FeedbackItem::getPriority))
            .limit(5)
            .collect(Collectors.toList());
    }

    public String generateStrengthMessage(List<String> strengths) {
        if (strengths == null || strengths.isEmpty()) {
            return "GitHub 활동을 시작하여 역량을 쌓아가고 있습니다.";
        }
        Map<String, String> labels = Map.of(
            "activity",      "꾸준한 커밋 활동",
            "diversity",     "다양한 기술 스택 경험",
            "collaboration", "활발한 협업 참여",
            "persistence",   "장기적인 활동 지속성"
        );
        String strengthList = strengths.stream()
            .map(s -> labels.getOrDefault(s, s))
            .collect(Collectors.joining(", "));
        return String.format("특히 %s 부분에서 뛰어난 역량을 보여주고 있습니다.", strengthList);
    }

    private FeedbackItem mapWeaknessToAction(String category) {
        String[] rule = FEEDBACK_RULES.get(category);
        if (rule == null) return null;
        int priority = switch (category) {
            case "activity"      -> 1;
            case "collaboration" -> 2;
            case "diversity"     -> 3;
            case "persistence"   -> 4;
            default              -> 5;
        };
        return new FeedbackItem(category, rule[0], rule[1], priority);
    }
}

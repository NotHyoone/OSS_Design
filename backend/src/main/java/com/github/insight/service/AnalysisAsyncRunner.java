package com.github.insight.service;

import com.github.insight.model.AnalysisRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @Async 메서드를 별도 빈으로 분리하여 Spring 프록시가 올바르게 적용되도록 함.
 * (같은 빈 내 self-invocation 시 @Async가 무시되는 문제 방지)
 */
@Service
public class AnalysisAsyncRunner {

    private final AnalysisService analysisService;

    public AnalysisAsyncRunner(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Async("analysisExecutor")
    public void run(AnalysisRequest req) {
        analysisService.executeAnalysis(req);
    }
}

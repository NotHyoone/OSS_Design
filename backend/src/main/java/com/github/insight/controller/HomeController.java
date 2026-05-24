package com.github.insight.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HomeController {

    /** 루트 경로 → index.html 리다이렉트 */
    @GetMapping("/")
    public RedirectView root() {
        return new RedirectView("/index.html");
    }

    /**
     * 루트 경로 - API 정보 제공
     */
    @GetMapping("/api/info")
    public ResponseEntity<?> info() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 200);
        response.put("message", "GitHub Activity Insight API");
        response.put("version", "1.0.0");
        response.put("endpoints", Map.ofEntries(
            Map.entry("auth", "/auth"),
            Map.entry("analysis", "/api/analysis"),
            Map.entry("github", "/api/github"),
            Map.entry("info", "/api/info"),
            Map.entry("h2-console", "/h2-console")
        ));
        return ResponseEntity.ok(response);
    }
}


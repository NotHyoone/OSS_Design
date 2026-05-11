package com.github.insight.dto;

/** GET /api/github/validate 응답 */
public record ValidateResponse(boolean valid, String avatarUrl, String message) {}

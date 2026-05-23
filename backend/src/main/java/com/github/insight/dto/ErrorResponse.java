package com.github.insight.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private int status;
    private String message;
    private String error;
    private String details;
    private LocalDateTime timestamp;
    private String path;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String message, String error) {
        this();
        this.status = status;
        this.message = message;
        this.error = error;
    }

    public ErrorResponse(int status, String message, String error, String details) {
        this(status, message, error);
        this.details = details;
    }

    public static ErrorResponse of(int status, String message, String error) {
        return new ErrorResponse(status, message, error);
    }

    public static ErrorResponse of(int status, String message, String error, String details) {
        return new ErrorResponse(status, message, error, details);
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}

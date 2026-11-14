package com.freightfox.dispatch.dto;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {

    private String status;
    private String error;
    private List<String> details;
    private Instant timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String status, String error, List<String> details) {
        this.status = status;
        this.error = error;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
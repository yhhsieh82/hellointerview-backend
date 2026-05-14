package com.hellointerview.backend.service.feedback;

/**
 * Identifies the active LLM provider/model pair for Strategy B admission budgeting and metrics tags.
 */
public record AdmissionWorkloadKey(String provider, String model) {

    public String compositeKey() {
        return provider + "::" + model;
    }
}

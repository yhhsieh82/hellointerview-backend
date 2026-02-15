package com.hellointerview.backend.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum QuestionType {
    FUNCTIONAL_REQ("Functional Req"),
    NON_FUNCTIONAL_REQ("Non-Functional Req"),
    ENTITIES("Entities"),
    API("API"),
    HIGH_LEVEL_DESIGN("High Level Design"),
    DEEP_DIVE("Deep Dive");

    private final String displayName;

    QuestionType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }
}

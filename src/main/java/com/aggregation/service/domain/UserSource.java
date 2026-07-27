package com.aggregation.service.domain;

public enum UserSource {
    POSTGRESQL("PostgreSQL"),
    MONGODB("MongoDB");

    private final String displayName;

    UserSource(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

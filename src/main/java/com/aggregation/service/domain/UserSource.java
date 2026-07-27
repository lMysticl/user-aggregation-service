package com.aggregation.service.domain;

import java.util.Arrays;

public enum UserSource {
    POSTGRESQL("postgresql", "PostgreSQL"),
    MONGODB("mongodb", "MongoDB");

    private final String apiValue;
    private final String displayName;

    UserSource(String apiValue, String displayName) {
        this.apiValue = apiValue;
        this.displayName = displayName;
    }

    public String apiValue() {
        return apiValue;
    }

    public String displayName() {
        return displayName;
    }

    public static UserSource fromApiValue(String value) {
        return Arrays.stream(values())
                .filter(source -> source.apiValue.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown user source: " + value
                ));
    }
}

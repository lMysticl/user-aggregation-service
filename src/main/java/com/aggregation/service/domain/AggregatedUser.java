package com.aggregation.service.domain;

import java.util.Objects;

public record AggregatedUser(
        UserSource source,
        String sourceId,
        String username,
        String firstName,
        String lastName) {

    public AggregatedUser {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
    }
}

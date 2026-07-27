package com.aggregation.service.application;

import com.aggregation.service.domain.AggregatedUser;

import java.util.List;

public record UserPage(
        List<AggregatedUser> items,
        int page,
        int size,
        boolean hasNext) {

    public UserPage {
        items = List.copyOf(items);
    }
}

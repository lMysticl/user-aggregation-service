package com.aggregation.service.dto;

import com.aggregation.service.application.UserPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "A stable, bounded page of aggregated users")
public record UserPageResponse(
        List<AggregatedUserResponse> items,
        int page,
        int size,
        boolean hasNext) {

    public static UserPageResponse from(UserPage page) {
        return new UserPageResponse(
                page.items().stream()
                        .map(AggregatedUserResponse::from)
                        .toList(),
                page.page(),
                page.size(),
                page.hasNext()
        );
    }
}

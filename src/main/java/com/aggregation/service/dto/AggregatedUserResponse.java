package com.aggregation.service.dto;

import com.aggregation.service.domain.AggregatedUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "User with an unambiguous source-local identity")
public record AggregatedUserResponse(
        @Schema(description = "Source system", allowableValues = {"postgresql", "mongodb"})
        String source,
        @Schema(description = "Identifier inside the source system")
        String sourceId,
        String username,
        String firstName,
        String lastName) {

    public static AggregatedUserResponse from(AggregatedUser user) {
        return new AggregatedUserResponse(
                user.source().apiValue(),
                user.sourceId(),
                user.username(),
                user.firstName(),
                user.lastName()
        );
    }
}

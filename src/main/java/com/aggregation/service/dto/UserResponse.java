package com.aggregation.service.dto;

import com.aggregation.service.domain.AggregatedUser;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Backward-compatible v1 user response")
public record UserResponse(
        @Schema(description = "Source-local user identifier")
        String id,
        @Schema(description = "Username", example = "johndoe")
        String username,
        @Schema(description = "First name", example = "John")
        String name,
        @Schema(description = "Last name", example = "Doe")
        String surname) {

    public static UserResponse from(AggregatedUser user) {
        return new UserResponse(
                user.sourceId(),
                user.username(),
                user.firstName(),
                user.lastName()
        );
    }
}

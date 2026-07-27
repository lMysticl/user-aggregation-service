package com.aggregation.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User data accepted by the create-user endpoint")
public record CreateUserRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "Unique username in the relational store", example = "johndoe")
        String username,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "First name", example = "John")
        String name,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Last name", example = "Doe")
        String surname
) {
}

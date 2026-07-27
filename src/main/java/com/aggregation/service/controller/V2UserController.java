package com.aggregation.service.controller;

import com.aggregation.service.application.CreateUserCommand;
import com.aggregation.service.application.UserAggregationService;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import com.aggregation.service.dto.AggregatedUserResponse;
import com.aggregation.service.dto.CreateUserV2Request;
import com.aggregation.service.dto.UserPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v2/users")
@Tag(name = "Users v2", description = "Bounded user aggregation with source provenance")
@RequiredArgsConstructor
@Validated
public class V2UserController {
    private final UserAggregationService userAggregationService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a stable page of users from all configured sources")
    public ResponseEntity<UserPageResponse> getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(UserPageResponse.from(
                userAggregationService.searchUsers(username, name, page, size)
        ));
    }

    @GetMapping(
            value = "/{source}/{sourceId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Get a user by source and source-local ID")
    public ResponseEntity<AggregatedUserResponse> getUser(
            @PathVariable UserSource source,
            @PathVariable String sourceId) {
        return ResponseEntity.ok(AggregatedUserResponse.from(
                userAggregationService.getUser(source, sourceId)
        ));
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Create a PostgreSQL user and return its source identity")
    public ResponseEntity<AggregatedUserResponse> createUser(
            @Valid @RequestBody CreateUserV2Request request) {
        AggregatedUser createdUser = userAggregationService.createUser(
                new CreateUserCommand(
                        request.username(),
                        request.firstName(),
                        request.lastName()
                )
        );
        return ResponseEntity
                .created(URI.create(
                        "/api/v2/users/"
                                + createdUser.source().apiValue()
                                + "/"
                                + createdUser.sourceId()
                ))
                .body(AggregatedUserResponse.from(createdUser));
    }
}

package com.aggregation.service.application;

public record CreateUserCommand(
        String username,
        String firstName,
        String lastName) {
}

package com.aggregation.service.exception;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final String userId;

    public UserNotFoundException(String userId) {
        super("User not found: " + userId);
        this.userId = userId;
    }
}

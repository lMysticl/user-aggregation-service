package com.aggregation.service.exception;

import com.aggregation.service.domain.UserSource;
import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final UserSource source;
    private final String userId;

    public UserNotFoundException(String userId) {
        this(UserSource.POSTGRESQL, userId);
    }

    public UserNotFoundException(UserSource source, String userId) {
        super("User not found in " + source.displayName() + ": " + userId);
        this.source = source;
        this.userId = userId;
    }
}

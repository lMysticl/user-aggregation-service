package com.aggregation.service.exception;

import lombok.Getter;

@Getter
public class SourceUnavailableException extends RuntimeException {
    private final String source;

    public SourceUnavailableException(String source, Throwable cause) {
        super(source + " data source is unavailable", cause);
        this.source = source;
    }
}

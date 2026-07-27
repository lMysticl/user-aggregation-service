package com.aggregation.service.application.port;

import com.aggregation.service.domain.UserSource;

public interface UserSourceMetrics {

    QueryTimer start(UserSource source);

    enum Outcome {
        SUCCESS("success"),
        FAILURE("failure"),
        TIMEOUT("timeout"),
        REJECTED("rejected");

        private final String tagValue;

        Outcome(String tagValue) {
            this.tagValue = tagValue;
        }

        public String tagValue() {
            return tagValue;
        }
    }

    @FunctionalInterface
    interface QueryTimer {
        void stop(Outcome outcome);
    }

    static UserSourceMetrics noop() {
        return source -> outcome -> {
        };
    }
}

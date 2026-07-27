package com.aggregation.service.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "aggregation")
public class AggregationProperties {

    @NotNull
    private Duration queryTimeout = Duration.ofSeconds(2);

    @Valid
    private ExecutorProperties executor = new ExecutorProperties();

    @Getter
    @Setter
    public static class ExecutorProperties {
        @Min(1)
        private int corePoolSize = 4;

        @Min(1)
        private int maxPoolSize = 8;

        @Min(0)
        private int queueCapacity = 100;
    }
}

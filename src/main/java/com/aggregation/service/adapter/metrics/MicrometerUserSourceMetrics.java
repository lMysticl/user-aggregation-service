package com.aggregation.service.adapter.metrics;

import com.aggregation.service.application.port.UserSourceMetrics;
import com.aggregation.service.domain.UserSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class MicrometerUserSourceMetrics implements UserSourceMetrics {
    public static final String QUERY_METRIC = "user.aggregation.source.query";

    private final MeterRegistry meterRegistry;

    @Override
    public QueryTimer start(UserSource source) {
        Timer.Sample sample = Timer.start(meterRegistry);
        AtomicBoolean stopped = new AtomicBoolean();
        return outcome -> {
            if (stopped.compareAndSet(false, true)) {
                sample.stop(Timer.builder(QUERY_METRIC)
                        .description("User source query duration")
                        .tag("source", source.apiValue())
                        .tag("outcome", outcome.tagValue())
                        .register(meterRegistry));
            }
        };
    }
}

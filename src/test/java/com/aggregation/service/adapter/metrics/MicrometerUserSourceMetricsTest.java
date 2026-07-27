package com.aggregation.service.adapter.metrics;

import com.aggregation.service.application.port.UserSourceMetrics;
import com.aggregation.service.domain.UserSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerUserSourceMetricsTest {

    @Test
    void recordsOneLowCardinalityTimerPerSourceAndOutcome() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerUserSourceMetrics metrics = new MicrometerUserSourceMetrics(registry);
        UserSourceMetrics.QueryTimer timer = metrics.start(UserSource.MONGODB);

        timer.stop(UserSourceMetrics.Outcome.TIMEOUT);
        timer.stop(UserSourceMetrics.Outcome.SUCCESS);

        assertThat(registry.find(MicrometerUserSourceMetrics.QUERY_METRIC)
                .tags("source", "mongodb", "outcome", "timeout")
                .timer())
                .isNotNull()
                .satisfies(recorded -> assertThat(recorded.count()).isEqualTo(1));
        assertThat(registry.find(MicrometerUserSourceMetrics.QUERY_METRIC)
                .tags("outcome", "success")
                .timer())
                .isNull();
    }
}

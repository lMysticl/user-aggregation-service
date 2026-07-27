package com.aggregation.service.application.port;

import com.aggregation.service.application.CreateUserCommand;
import com.aggregation.service.domain.AggregatedUser;

public interface UserWriter {
    AggregatedUser create(CreateUserCommand command);
}

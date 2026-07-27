package com.aggregation.service.application.port;

import com.aggregation.service.application.UserSearchCriteria;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;

import java.util.List;
import java.util.Optional;

public interface UserReadSource {
    UserSource source();

    List<AggregatedUser> search(UserSearchCriteria criteria, int limit);

    Optional<AggregatedUser> findById(String sourceId);
}

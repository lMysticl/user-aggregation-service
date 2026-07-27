package com.aggregation.service.config;

import com.aggregation.service.domain.UserSource;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class UserSourceConverter implements Converter<String, UserSource> {
    @Override
    public UserSource convert(String source) {
        return UserSource.fromApiValue(source);
    }
}

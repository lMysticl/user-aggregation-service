package com.aggregation.service.adapter.persistence.mongo;

import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "users")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MongoUserDocument {
    @Id
    private String id;

    @Field("username")
    private String username;

    @Field("name")
    private String firstName;

    @Field("surname")
    private String lastName;

    public AggregatedUser toDomain() {
        return new AggregatedUser(
                UserSource.MONGODB,
                id,
                username,
                firstName,
                lastName
        );
    }
}

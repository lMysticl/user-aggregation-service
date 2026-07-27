package com.aggregation.service.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User entity representing a person in the system")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Username for login", example = "johndoe", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Schema(description = "User's first name", example = "John", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false, length = 100)
    private String name;

    @Schema(description = "User's last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @Column(nullable = false, length = 100)
    private String surname;
}

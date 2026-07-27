package com.aggregation.service.controller;

import com.aggregation.service.exception.SourceUnavailableException;
import com.aggregation.service.model.User;
import com.aggregation.service.service.UserAggregationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@ExtendWith(OutputCaptureExtension.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAggregationService userAggregationService;

    @Test
    void createUserRejectsBlankRequiredFields() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "name": " ",
                                  "surname": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request data"));
    }

    @Test
    void createUserReturnsCreatedForValidRequest() throws Exception {
        when(userAggregationService.createUser(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId("123e4567-e89b-12d3-a456-426614174000");
                    return user;
                });

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "johndoe",
                                  "name": "John",
                                  "surname": "Doe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/users/123e4567-e89b-12d3-a456-426614174000"
                ))
                .andExpect(jsonPath("$.id").value("123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void getUsersReturnsServiceUnavailableWithoutLeakingCause(
            CapturedOutput output) throws Exception {
        when(userAggregationService.searchUsers(null, null))
                .thenThrow(new SourceUnavailableException(
                        "MongoDB",
                        new IllegalStateException("mongodb://admin:secret@internal-host")
                ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Data source unavailable"))
                .andExpect(jsonPath("$.details").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("secret")
                        )
                ));
        org.assertj.core.api.Assertions.assertThat(output)
                .doesNotContain("mongodb://admin:secret@internal-host");
    }

    @Test
    void unexpectedErrorsReturnCorrelationIdWithoutLeakingCause(
            CapturedOutput output) throws Exception {
        when(userAggregationService.searchUsers(null, null))
                .thenThrow(new IllegalStateException("database password is secret"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.details").value(
                        org.hamcrest.Matchers.allOf(
                                org.hamcrest.Matchers.containsString("error ID:"),
                                org.hamcrest.Matchers.not(
                                        org.hamcrest.Matchers.containsString("secret")
                                )
                        )
                ));
        org.assertj.core.api.Assertions.assertThat(output)
                .doesNotContain("database password is secret");
    }
}

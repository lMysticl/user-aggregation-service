package com.aggregation.service.controller;

import com.aggregation.service.application.CreateUserCommand;
import com.aggregation.service.application.UserAggregationService;
import com.aggregation.service.application.UserPage;
import com.aggregation.service.domain.AggregatedUser;
import com.aggregation.service.domain.UserSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(V2UserController.class)
class V2UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAggregationService userAggregationService;

    @Test
    void searchReturnsProvenanceAndPageMetadata() throws Exception {
        when(userAggregationService.searchUsers(null, null, 0, 20))
                .thenReturn(new UserPage(
                        List.of(new AggregatedUser(
                                UserSource.MONGODB,
                                "mongo-1",
                                "johndoe",
                                "John",
                                "Doe"
                        )),
                        0,
                        20,
                        true
                ));

        mockMvc.perform(get("/api/v2/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].source").value("mongodb"))
                .andExpect(jsonPath("$.items[0].sourceId").value("mongo-1"))
                .andExpect(jsonPath("$.items[0].firstName").value("John"))
                .andExpect(jsonPath("$.items[0].lastName").value("Doe"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void invalidPageAndSizeReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v2/users?page=-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v2/users?size=101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sourcePathUsesTheRequestedAdapter() throws Exception {
        AggregatedUser user = new AggregatedUser(
                UserSource.MONGODB,
                "mongo-1",
                "johndoe",
                "John",
                "Doe"
        );
        when(userAggregationService.getUser(UserSource.MONGODB, "mongo-1"))
                .thenReturn(user);

        mockMvc.perform(get("/api/v2/users/mongodb/mongo-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("mongodb"))
                .andExpect(jsonPath("$.sourceId").value("mongo-1"));

        verify(userAggregationService).getUser(UserSource.MONGODB, "mongo-1");
    }

    @Test
    void createReturnsAResolvableSourceAwareLocation() throws Exception {
        when(userAggregationService.createUser(any(CreateUserCommand.class)))
                .thenReturn(new AggregatedUser(
                        UserSource.POSTGRESQL,
                        "pg-1",
                        "johndoe",
                        "John",
                        "Doe"
                ));

        mockMvc.perform(post("/api/v2/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "johndoe",
                                  "firstName": "John",
                                  "lastName": "Doe"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v2/users/postgresql/pg-1"
                ))
                .andExpect(jsonPath("$.source").value("postgresql"))
                .andExpect(jsonPath("$.sourceId").value("pg-1"));
    }
}

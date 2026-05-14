package com.campushub.bootstrap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.campushub.test.PostgresTestcontainer;
import com.campushub.test.RedisTestcontainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CampushubApplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {
        if (PostgresTestcontainer.isDockerAvailable()) {
            PostgresTestcontainer.register(registry);
            RedisTestcontainer.register(registry);
            return;
        }

        registry.add(
            "spring.datasource.url",
            () -> "jdbc:h2:mem:campushub;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;"
                + "INIT=CREATE SCHEMA IF NOT EXISTS identity\\;"
                + "CREATE SCHEMA IF NOT EXISTS catalog\\;"
                + "CREATE SCHEMA IF NOT EXISTS trading\\;"
                + "CREATE SCHEMA IF NOT EXISTS messaging\\;"
                + "CREATE SCHEMA IF NOT EXISTS media"
        );
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Test
    void shouldRegisterUserAndListItem() throws Exception {
        String userResponse = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"a@b.c",
                      "password":"pw12345",
                      "displayName":"Test"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("a@b.c"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        mockMvc.perform(post("/api/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sellerId":%d,
                      "title":"Bike",
                      "description":"Fast bike",
                      "priceAmount":199.99,
                      "priceCurrency":"USD"
                    }
                    """.formatted(userId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Bike"));
    }

    @Test
    void shouldShowSwaggerAndGetItemById() throws Exception {
        String userResponse = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email":"show@b.c",
                      "password":"pw12345",
                      "displayName":"Show"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long userId = objectMapper.readTree(userResponse).get("id").asLong();

        String itemResponse = mockMvc.perform(post("/api/v1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sellerId":%d,
                      "title":"Desk Lamp",
                      "description":"Warm light",
                      "priceAmount":29.99,
                      "priceCurrency":"USD"
                    }
                    """.formatted(userId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode itemNode = objectMapper.readTree(itemResponse);
        long itemId = itemNode.get("id").asLong();

        mockMvc.perform(get("/api/v1/items/{id}", itemId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Desk Lamp"));

        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection());
    }
}

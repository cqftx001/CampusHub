package com.campushub.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public final class PostgresTestcontainer {

    private static final PostgreSQLContainer<?> CONTAINER =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("campushub")
            .withUsername("campushub")
            .withPassword("campushub");

    private PostgresTestcontainer() {
    }

    public static PostgreSQLContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    public static void register(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> container = container();
        registry.add("spring.datasource.url", container::getJdbcUrl);
        registry.add("spring.datasource.username", container::getUsername);
        registry.add("spring.datasource.password", container::getPassword);
    }

    public static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException ex) {
            return false;
        }
    }
}

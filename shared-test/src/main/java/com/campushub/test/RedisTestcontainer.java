package com.campushub.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public final class RedisTestcontainer {

    private static final GenericContainer<?> CONTAINER =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedisTestcontainer() {
    }

    public static GenericContainer<?> container() {
        if (!CONTAINER.isRunning()) {
            CONTAINER.start();
        }
        return CONTAINER;
    }

    public static void register(DynamicPropertyRegistry registry) {
        GenericContainer<?> container = container();
        registry.add("spring.data.redis.host", container::getHost);
        registry.add("spring.data.redis.port", () -> container.getMappedPort(6379));
    }
}

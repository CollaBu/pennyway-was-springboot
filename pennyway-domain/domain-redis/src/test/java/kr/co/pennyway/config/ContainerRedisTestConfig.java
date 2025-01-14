package kr.co.pennyway.config;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@DisplayName("Container Redis 설정")
public abstract class ContainerRedisTestConfig {
    private static final String REDIS_CONTAINER_NAME = "redis:7.4";
    private static final GenericContainer<?> REDIS_CONTAINER;

    static {
        REDIS_CONTAINER =
                new GenericContainer<>(DockerImageName.parse(REDIS_CONTAINER_NAME))
                        .withExposedPorts(6379)
                        .withCommand("redis-server", "--requirepass testpass")
                        .withReuse(true);

        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    public static void setRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
        registry.add("spring.data.redis.password", () -> "testpass");
    }
}

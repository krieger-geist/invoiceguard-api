package com.invoiceguard.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Every integration test extends this. Spins up real PostgreSQL and Redis
 * containers once per Maven test run. They deliberately outlive every test
 * class because Spring caches this integration-test application context. If
 * JUnit stopped the containers after one class, the cached context would keep
 * trying to use a now-closed mapped port in the next class.
 *
 * <p>The tests therefore run against the actual database engine and
 * cache Flyway migrations will run against in production — an H2-in-memory
 * substitute would silently pass tests against SQL that breaks on real
 * Postgres (e.g. our {@code @SQLRestriction}, partial unique indexes,
 * {@code NUMERIC} precision).
 *
 * <p>Requires Docker to be available wherever these tests run — see the
 * CI workflow, which provisions it automatically on the GitHub Actions
 * runner. If Docker isn't available locally, these tests fail to start the
 * containers rather than silently skipping, which is deliberate: a
 * "passing" integration test suite that never actually ran isn't a passing
 * test suite.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("invoiceguard_test")
            .withUsername("test")
            .withPassword("test");

    static final com.redis.testcontainers.RedisContainer redis =
            new com.redis.testcontainers.RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getFirstMappedPort());
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;
}

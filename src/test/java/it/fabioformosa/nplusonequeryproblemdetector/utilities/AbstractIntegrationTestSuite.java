package it.fabioformosa.nplusonequeryproblemdetector.utilities;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

/**
 * It launches a postgresql testcontainer if the spring active profile is "postgresql"
 * otherwise it configures a h2-based datasource (make sure to import to h2 dependency)
 * <dependency>
 * <groupId>com.h2database</groupId>
 * <artifactId>h2</artifactId>
 * </dependency>
 */
@SpringBootTest
@ContextConfiguration(initializers = {AbstractIntegrationTestSuite.Initializer.class})
public abstract class AbstractIntegrationTestSuite {

    private static final String POSTGRESQL_SPRING_PROFILE = "postgresql";
    private static final String POSTGRESQL_DOCKER_IMAGE = "postgres:14.5";
    private static PostgreSQLContainer postgresqlContainer = null;

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            if (Arrays.stream(configurableApplicationContext.getEnvironment().getActiveProfiles()).anyMatch(POSTGRESQL_SPRING_PROFILE::equals)) {
                getOrRunAPostgresqlContainer();
                initPostgresqlContainer(configurableApplicationContext);
            } else initH2Datasource(configurableApplicationContext);

        }

        private static void getOrRunAPostgresqlContainer() {
            if (postgresqlContainer == null) {
                postgresqlContainer = (PostgreSQLContainer) new PostgreSQLContainer(DockerImageName.parse(POSTGRESQL_DOCKER_IMAGE))
                        .withInitScript("db/postgres-init.sql");
                postgresqlContainer.start();
            }
        }

        private void initPostgresqlContainer(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.driver-class-name=" + postgresqlContainer.getDriverClassName(),
                    "spring.datasource.url=" + postgresqlContainer.getJdbcUrl(),
                    "spring.datasource.username=" + postgresqlContainer.getUsername(),
                    "spring.datasource.password=" + postgresqlContainer.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }

        private void initH2Datasource(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.datasource.url=jdbc:h2:mem:db;DB_CLOSE_DELAY=-1",
                    "spring.datasource.username=sa",
                    "spring.datasource.password=sa"
            ).applyTo(configurableApplicationContext.getEnvironment());
        }


    }
}

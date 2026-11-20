package dev.gymscout;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** One throwaway PostGIS per test context (needs Docker or Podman). */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgisContainer() {
    return new PostgreSQLContainer(
        DockerImageName.parse("postgis/postgis:17-3.5-alpine")
            .asCompatibleSubstituteFor("postgres"));
  }
}

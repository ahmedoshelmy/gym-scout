package dev.gymscout;

import org.springframework.boot.SpringApplication;

/** Runs the app locally against a Testcontainers PostGIS: {@code ./mvnw spring-boot:test-run}. */
public class TestGymScoutApplication {

  public static void main(String[] args) {
    SpringApplication.from(GymScoutApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}

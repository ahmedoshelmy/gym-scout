package dev.gymscout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Gym Scout: a modular monolith on PostGIS (see {@code docs/adr}). */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GymScoutApplication {

  public static void main(String[] args) {
    SpringApplication.run(GymScoutApplication.class, args);
  }
}

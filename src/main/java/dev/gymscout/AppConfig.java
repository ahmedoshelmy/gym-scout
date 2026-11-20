package dev.gymscout;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cross-cutting beans. */
@Configuration(proxyBeanMethods = false)
class AppConfig {

  /** Injected wherever "today" matters, so tests can pin the date. */
  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}

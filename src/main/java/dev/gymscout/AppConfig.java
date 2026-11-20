package dev.gymscout;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cross-cutting beans. */
@Configuration(proxyBeanMethods = false)
class AppConfig {

  /**
   * Injected wherever "today" matters, so tests can pin the date. The zone comes from {@code
   * gymscout.zone}, not from the host or container TZ, so a session logged at 00:30 local time
   * lands on the right day wherever the API runs. An unknown zone id fails startup.
   */
  @Bean
  Clock clock(@Value("${gymscout.zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }
}

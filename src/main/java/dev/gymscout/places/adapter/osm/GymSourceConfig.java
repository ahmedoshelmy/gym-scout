package dev.gymscout.places.adapter.osm;

import dev.gymscout.outbound.OutboundProperties;
import dev.gymscout.outbound.PoliteCaller;
import dev.gymscout.places.application.GymSource;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Picks the gym source: {@code gymscout.places.source=synthetic} (default) or {@code overpass}. */
@Configuration(proxyBeanMethods = false)
class GymSourceConfig {

  @Bean
  @ConditionalOnProperty(
      name = "gymscout.places.source",
      havingValue = "synthetic",
      matchIfMissing = true)
  GymSource syntheticGymSource() {
    return new SyntheticGymSource();
  }

  @Bean
  @ConditionalOnProperty(name = "gymscout.places.source", havingValue = "overpass")
  GymSource overpassGymSource(
      @Value("${gymscout.places.overpass-url:https://overpass-api.de/api/interpreter}") String url,
      OutboundProperties props,
      MeterRegistry meters) {
    var factory = new JdkClientHttpRequestFactory();
    factory.setReadTimeout(props.readTimeout());
    RestClient http =
        RestClient.builder()
            .baseUrl(url)
            .requestFactory(factory)
            .defaultHeader("User-Agent", props.userAgent())
            .build();
    return new OverpassGymSource(http, new PoliteCaller("overpass", props, meters));
  }
}

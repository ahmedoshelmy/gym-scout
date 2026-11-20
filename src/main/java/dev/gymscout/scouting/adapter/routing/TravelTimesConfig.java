package dev.gymscout.scouting.adapter.routing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.gymscout.outbound.OutboundProperties;
import dev.gymscout.outbound.PoliteCaller;
import dev.gymscout.scouting.application.TravelTimes;
import dev.gymscout.scouting.domain.TravelEstimate;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Picks travel times: {@code gymscout.routing.source=straight-line} (default) or {@code osrm}. */
@Configuration(proxyBeanMethods = false)
class TravelTimesConfig {

  @Bean
  @ConditionalOnProperty(
      name = "gymscout.routing.source",
      havingValue = "straight-line",
      matchIfMissing = true)
  TravelTimes straightLineTravelTimes() {
    return new StraightLineTravelTimes();
  }

  @Bean
  @ConditionalOnProperty(name = "gymscout.routing.source", havingValue = "osrm")
  TravelTimes osrmTravelTimes(
      @Value("${gymscout.routing.osrm-url:https://router.project-osrm.org}") String url,
      @Value("${gymscout.routing.cache-ttl:24h}") Duration ttl,
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
    Cache<OsrmTravelTimes.RouteKey, TravelEstimate> cache =
        Caffeine.newBuilder().maximumSize(20_000).expireAfterWrite(ttl).recordStats().build();
    CaffeineCacheMetrics.monitor(meters, cache, "osrm-routes");
    return new OsrmTravelTimes(http, new PoliteCaller("osrm", props, meters), cache);
  }
}

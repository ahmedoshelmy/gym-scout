package dev.gymscout.outbound;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Policies shared by every upstream. Defaults follow the public servers' usage rules: at most one
 * request per second, a handful of retries with backoff, and a breaker that stops hammering a
 * struggling server.
 */
@ConfigurationProperties("gymscout.outbound")
public record OutboundProperties(
    @DefaultValue("gym-scout-demo") String userAgent,
    @DefaultValue("1") int requestsPerSecond,
    @DefaultValue("5s") Duration rateLimitWait,
    @DefaultValue("2") int maxConcurrentCalls,
    @DefaultValue("3") int maxAttempts,
    @DefaultValue("500ms") Duration firstBackoff,
    @DefaultValue("10s") Duration maxBackoff,
    @DefaultValue("10") int breakerWindow,
    @DefaultValue("4") int breakerMinimumCalls,
    @DefaultValue("50") float breakerFailureRatePercent,
    @DefaultValue("30s") Duration breakerOpenFor,
    @DefaultValue("10s") Duration readTimeout) {}

package dev.gymscout.outbound;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.functions.Either;
import io.github.resilience4j.micrometer.tagged.TaggedBulkheadMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRateLimiterMetrics;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Wraps calls to one upstream as Retry(CircuitBreaker(RateLimiter(Bulkhead(call)))): the bulkhead
 * caps parallel calls, the limiter spaces them out, the breaker fails fast while the upstream is
 * sick and every retry goes back through all three. Only 429, 5xx and I/O errors are retried; a 4xx
 * means our request is wrong and retrying would be rude.
 */
public final class PoliteCaller {

  private final String upstream;
  private final Retry retry;
  private final CircuitBreaker breaker;
  private final RateLimiter limiter;
  private final Bulkhead bulkhead;

  public PoliteCaller(String upstream, OutboundProperties props, MeterRegistry meters) {
    this.upstream = upstream;
    var retries =
        RetryRegistry.of(
            RetryConfig.<Object>custom()
                .maxAttempts(props.maxAttempts())
                .retryOnException(PoliteCaller::isTransient)
                .intervalBiFunction(backoff(props))
                .build());
    var breakers =
        CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .slidingWindowSize(props.breakerWindow())
                .minimumNumberOfCalls(props.breakerMinimumCalls())
                .failureRateThreshold(props.breakerFailureRatePercent())
                .waitDurationInOpenState(props.breakerOpenFor())
                .recordException(PoliteCaller::isTransient)
                .build());
    var limiters =
        RateLimiterRegistry.of(
            RateLimiterConfig.custom()
                .limitForPeriod(props.requestsPerSecond())
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(props.rateLimitWait())
                .build());
    var bulkheads =
        BulkheadRegistry.of(
            BulkheadConfig.custom()
                .maxConcurrentCalls(props.maxConcurrentCalls())
                .maxWaitDuration(props.rateLimitWait())
                .build());
    this.retry = retries.retry(upstream);
    this.breaker = breakers.circuitBreaker(upstream);
    this.limiter = limiters.rateLimiter(upstream);
    this.bulkhead = bulkheads.bulkhead(upstream);
    TaggedRetryMetrics.ofRetryRegistry(retries).bindTo(meters);
    TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(breakers).bindTo(meters);
    TaggedRateLimiterMetrics.ofRateLimiterRegistry(limiters).bindTo(meters);
    TaggedBulkheadMetrics.ofBulkheadRegistry(bulkheads).bindTo(meters);
  }

  /** Runs the call under all four policies; throws {@link UpstreamUnavailableException}. */
  public <T> T call(Supplier<T> call) {
    Supplier<T> guarded = Bulkhead.decorateSupplier(bulkhead, call);
    guarded = RateLimiter.decorateSupplier(limiter, guarded);
    guarded = CircuitBreaker.decorateSupplier(breaker, guarded);
    guarded = Retry.decorateSupplier(retry, guarded);
    try {
      return guarded.get();
    } catch (RuntimeException e) {
      throw new UpstreamUnavailableException(upstream, e);
    }
  }

  public CircuitBreaker.State breakerState() {
    return breaker.getState();
  }

  static boolean isTransient(Throwable error) {
    if (error instanceof ResourceAccessException) {
      return true;
    }
    if (error instanceof RestClientResponseException response) {
      HttpStatusCode status = response.getStatusCode();
      return status.value() == 429 || status.is5xxServerError();
    }
    return false;
  }

  /** Exponential backoff, but a server's own {@code Retry-After} (seconds) wins, within a cap. */
  private static IntervalBiFunction<Object> backoff(OutboundProperties props) {
    return (attempt, outcome) -> {
      long exponential = props.firstBackoff().toMillis() * (1L << Math.min(attempt - 1, 10));
      long wait = Math.min(exponential, props.maxBackoff().toMillis());
      return retryAfterMillis(outcome)
          .map(asked -> Math.min(asked, props.maxBackoff().toMillis()))
          .orElse(wait);
    };
  }

  private static Optional<Long> retryAfterMillis(Either<Throwable, Object> outcome) {
    if (outcome.isLeft()
        && outcome.getLeft() instanceof RestClientResponseException response
        && response.getResponseHeaders() != null) {
      String header = response.getResponseHeaders().getFirst("Retry-After");
      if (header != null && header.strip().matches("\\d{1,4}")) {
        return Optional.of(Long.parseLong(header.strip()) * 1000);
      }
    }
    return Optional.empty();
  }
}

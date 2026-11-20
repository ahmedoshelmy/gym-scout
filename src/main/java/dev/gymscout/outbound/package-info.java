/**
 * Outbound etiquette for free public APIs: one {@link dev.gymscout.outbound.PoliteCaller} per
 * upstream wraps every call in a bulkhead, a rate limiter, a circuit breaker and a retry
 * (Resilience4j), all reported to Micrometer. Open to every module.
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN)
package dev.gymscout.outbound;

import org.springframework.modulith.ApplicationModule;

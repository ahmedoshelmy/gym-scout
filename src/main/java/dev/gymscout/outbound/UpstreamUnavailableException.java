package dev.gymscout.outbound;

/**
 * The upstream could not answer within our policies (breaker open, limiter full, retries spent).
 */
public class UpstreamUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UpstreamUnavailableException(String upstream, Throwable cause) {
    super(upstream + " is unavailable: " + cause.getMessage(), cause);
  }
}

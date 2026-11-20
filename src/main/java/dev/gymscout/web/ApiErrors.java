package dev.gymscout.web;

import dev.gymscout.outbound.UpstreamUnavailableException;
import dev.gymscout.shared.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain errors to RFC 9457 problem details. */
@RestControllerAdvice
class ApiErrors {

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail notFound(NotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail invalid(IllegalArgumentException e) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
  }

  @ExceptionHandler(UpstreamUnavailableException.class)
  ProblemDetail upstream(UpstreamUnavailableException e) {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Map data source unavailable",
        "The public map server did not answer; try again in a minute.");
  }

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }
}

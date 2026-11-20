package dev.gymscout.memberships.adapter.web;

import dev.gymscout.memberships.application.MembershipService;
import dev.gymscout.memberships.application.MembershipView;
import dev.gymscout.memberships.domain.Membership;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memberships")
@Tag(name = "Memberships", description = "Your gym, your visits, your real cost per visit")
class MembershipController {

  private final MembershipService memberships;

  MembershipController(MembershipService memberships) {
    this.memberships = memberships;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  MembershipView start(@Valid @RequestBody StartRequest body) {
    return memberships.start(
        new Membership(
            null,
            body.gymId(),
            body.planName(),
            body.priceMinor(),
            body.currency(),
            body.startsOn(),
            body.endsOn()));
  }

  @GetMapping("/current")
  @Operation(summary = "The membership covering today, or 204 when there is none")
  ResponseEntity<MembershipView> current() {
    return memberships.current().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
  }

  @GetMapping
  List<MembershipView> all() {
    return memberships.all();
  }

  @PostMapping("/{id}/visits")
  @ResponseStatus(HttpStatus.CREATED)
  MembershipView visit(@PathVariable UUID id, @RequestBody(required = false) VisitRequest body) {
    return memberships.addVisit(id, body == null ? null : body.on());
  }

  record StartRequest(
      @NotNull UUID gymId,
      @NotBlank String planName,
      @Min(0) long priceMinor,
      @NotBlank String currency,
      @NotNull LocalDate startsOn,
      @NotNull LocalDate endsOn) {}

  record VisitRequest(LocalDate on) {}
}

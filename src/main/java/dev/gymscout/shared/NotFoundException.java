package dev.gymscout.shared;

/** A requested gym, session, plan or membership does not exist. */
public class NotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NotFoundException(String message) {
    super(message);
  }
}

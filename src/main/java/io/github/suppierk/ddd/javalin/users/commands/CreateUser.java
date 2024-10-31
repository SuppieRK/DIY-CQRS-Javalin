package io.github.suppierk.ddd.javalin.users.commands;

import io.github.suppierk.ddd.cqrs.DomainCommand;
import io.github.suppierk.ddd.cqrs.DomainCommandHandler;
import io.github.suppierk.ddd.cqrs.DomainMessage;
import io.github.suppierk.example.tables.records.UsersRecord;
import io.javalin.openapi.Nullability;
import io.javalin.openapi.OpenApiPropertyType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain command to create new user in the system.
 *
 * <p>This is just an example of {@link DomainCommand} definition.
 *
 * <p>Note that extending {@link DomainCommand.Create} interface for record works nicely with Java
 * {@link Record}s.
 *
 * @param messageId to identify this command
 * @param createdAt is the time when this command was requested
 * @param username of the new user
 * @param password of the new user
 * @param email of the new user
 */
public record CreateUser(
    UUID messageId, Instant createdAt, String username, String password, String email)
    implements DomainCommand.Create<UUID, Instant> {
  /**
   * Alternative constructor, which automatically generates some of the {@link DomainMessage}
   * options.
   *
   * @param createUserRequest from the API to fetch parameters from
   */
  public CreateUser(CreateUserRequest createUserRequest) {
    this(
        UUID.randomUUID(),
        Instant.now(),
        createUserRequest.username,
        createUserRequest.password,
        createUserRequest.email);
  }

  /**
   * Publicly exposed Data Transfer Object which can be translated into the current {@link
   * DomainCommand}.
   *
   * @param username of the new user
   * @param password of the new user
   * @param email of the new user
   */
  public record CreateUserRequest(
      @OpenApiPropertyType(definedBy = String.class, nullability = Nullability.NOT_NULL)
          String username,
      @OpenApiPropertyType(definedBy = String.class, nullability = Nullability.NOT_NULL)
          String password,
      @OpenApiPropertyType(definedBy = String.class, nullability = Nullability.NOT_NULL)
          String email) {
    /** Post construct property validation. */
    public CreateUserRequest {
      if (username == null || username.isBlank()) {
        throw new IllegalArgumentException("Username cannot be null or blank");
      }

      if (password == null || password.isBlank()) {
        throw new IllegalArgumentException("Password cannot be null or blank");
      }

      if (email == null || email.isBlank()) {
        throw new IllegalArgumentException("Email cannot be null or blank");
      }
    }
  }

  /**
   * Respective {@link DomainCommandHandler} for the current {@link DomainCommand}.
   *
   * <p>This is just an example of {@link DomainCommandHandler} definition.
   *
   * <p>Note that since the library enforces 1-to-1 relationship between {@link DomainCommand} and
   * {@link DomainCommandHandler} it makes sense to have both defined in the same file - but this is
   * not required.
   */
  public static class Handler extends DomainCommandHandler.Create<CreateUser, UsersRecord> {
    public Handler() {
      super(CreateUser.class);
    }

    /** {@inheritDoc} */
    @Override
    protected UsersRecord fillBlankRecord(CreateUser command, UsersRecord blankRecord) {
      blankRecord.setId(UUID.randomUUID());
      blankRecord.setVersion(0);
      blankRecord.setCreatedAt(LocalDateTime.now());
      blankRecord.setUsername(command.username());
      blankRecord.setPassword(command.password());
      blankRecord.setEmail(command.email());
      return blankRecord;
    }
  }
}

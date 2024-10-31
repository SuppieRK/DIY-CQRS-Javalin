package io.github.suppierk.ddd.javalin.users.commands;

import static io.github.suppierk.example.Tables.USERS;

import io.github.suppierk.ddd.cqrs.DomainCommand;
import io.github.suppierk.ddd.cqrs.DomainCommandHandler;
import io.github.suppierk.ddd.cqrs.DomainMessage;
import io.github.suppierk.example.tables.records.UsersRecord;
import io.javalin.openapi.Nullability;
import io.javalin.openapi.OpenApiPropertyType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;

/**
 * Domain command to create new user in the system.
 *
 * <p>This is just an example of {@link DomainCommand} definition.
 *
 * <p>Note that extending {@link DomainCommand.Update} interface for record works nicely with Java
 * {@link Record}s.
 *
 * @param messageId to identify this command
 * @param createdAt is the time when this command was requested
 * @param id of the existing user to update
 * @param newUsername to set
 * @param newPassword to set
 * @param newEmail to set
 */
public record UpdateUser(
    UUID messageId,
    Instant createdAt,
    UUID id,
    Optional<String> newUsername,
    Optional<String> newPassword,
    Optional<String> newEmail)
    implements DomainCommand.Update<UUID, Instant> {
  /**
   * It is better to define {@link Condition} as a method rather than {@link Record} field due to
   * serialization issues.
   */
  @Override
  public Condition condition() {
    return USERS.ID.eq(id);
  }

  /**
   * Alternative constructor, which automatically generates some of the {@link DomainMessage}
   * options.
   *
   * @param id of the existing user to update
   * @param updateUserRequest from the API to fetch parameters from
   */
  public UpdateUser(UUID id, UpdateUserRequest updateUserRequest) {
    this(
        UUID.randomUUID(),
        Instant.now(),
        id,
        updateUserRequest.username.flatMap(
            value -> value.isBlank() ? Optional.empty() : Optional.of(value)),
        updateUserRequest.password.flatMap(
            value -> value.isBlank() ? Optional.empty() : Optional.of(value)),
        updateUserRequest.email.flatMap(
            value -> value.isBlank() ? Optional.empty() : Optional.of(value)));
  }

  /**
   * Publicly exposed Data Transfer Object which can be translated into the current {@link
   * DomainCommand}.
   *
   * @param username to set
   * @param password to set
   * @param email to set
   */
  public record UpdateUserRequest(
      @OpenApiPropertyType(definedBy = String.class, nullability = Nullability.NULLABLE)
          Optional<String> username,
      @OpenApiPropertyType(definedBy = String.class, nullability = Nullability.NULLABLE)
          Optional<String> password,
      @OpenApiPropertyType(definedBy = String.class, nullability = Nullability.NULLABLE)
          Optional<String> email) {}

  /**
   * Respective {@link DomainCommandHandler} for the current {@link DomainCommand}.
   *
   * <p>This is just an example of {@link DomainCommandHandler} definition.
   *
   * <p>Note that since the library enforces 1-to-1 relationship between {@link DomainCommand} and
   * {@link DomainCommandHandler} it makes sense to have both defined in the same file - but this is
   * not required.
   */
  public static class Handler extends DomainCommandHandler.Update<UpdateUser, UsersRecord> {
    public Handler() {
      super(UpdateUser.class);
    }

    /** {@inheritDoc} */
    @Override
    protected UsersRecord updateRecordValues(UpdateUser command, UsersRecord databaseRecord) {
      command.newUsername().ifPresent(databaseRecord::setUsername);
      command.newPassword().ifPresent(databaseRecord::setPassword);
      command.newEmail().ifPresent(databaseRecord::setEmail);
      return databaseRecord;
    }
  }
}

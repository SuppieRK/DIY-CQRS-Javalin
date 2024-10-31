package io.github.suppierk.ddd.javalin.users.commands;

import static io.github.suppierk.example.tables.Users.USERS;

import io.github.suppierk.ddd.cqrs.DomainCommand;
import io.github.suppierk.ddd.cqrs.DomainCommandHandler;
import io.github.suppierk.ddd.cqrs.DomainMessage;
import io.github.suppierk.example.tables.records.UsersRecord;
import java.time.Instant;
import java.util.UUID;
import org.jooq.Condition;

/**
 * Domain command to delete a user from the system.
 *
 * <p>This is just an example of {@link DomainCommand} definition.
 *
 * <p>Note that extending {@link DomainCommand.Delete} interface for record works nicely with Java
 * {@link Record}s.
 *
 * @param messageId to identify this command
 * @param createdAt is the time when this command was requested
 * @param id of the existing user to be deleted
 */
public record DeleteUser(UUID messageId, Instant createdAt, UUID id)
    implements DomainCommand.Delete<UUID, Instant> {

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
   * @param id of the existing user to be deleted
   */
  public DeleteUser(UUID id) {
    this(UUID.randomUUID(), Instant.now(), id);
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
  public static class Handler extends DomainCommandHandler.Delete<DeleteUser, UsersRecord> {
    public Handler() {
      super(DeleteUser.class);
    }
  }
}

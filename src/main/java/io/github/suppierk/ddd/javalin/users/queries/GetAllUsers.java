package io.github.suppierk.ddd.javalin.users.queries;

import static io.github.suppierk.example.Tables.USERS;

import io.github.suppierk.ddd.cqrs.DomainMessage;
import io.github.suppierk.ddd.cqrs.DomainQuery;
import io.github.suppierk.ddd.cqrs.DomainQueryHandler;
import io.github.suppierk.example.tables.records.UsersRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Domain query to fetch existing user from the system.
 *
 * <p>This is just an example of {@link DomainQuery} definition.
 *
 * <p>Note that extending {@link DomainQuery.Many} interface for record works nicely with Java
 * {@link Record}s.
 *
 * @param messageId to identify this command
 * @param createdAt is the time when this command was requested
 */
public record GetAllUsers(UUID messageId, Instant createdAt)
    implements DomainQuery.Many<UUID, Instant> {
  /**
   * Alternative constructor, which automatically generates some of the {@link DomainMessage}
   * options.
   */
  public GetAllUsers() {
    this(UUID.randomUUID(), Instant.now());
  }

  /**
   * Respective {@link DomainQueryHandler} for the current {@link DomainQuery}.
   *
   * <p>This is just an example of {@link DomainQueryHandler} definition.
   *
   * <p>Note that since the library enforces 1-to-1 relationship between {@link DomainQuery} and
   * {@link DomainQueryHandler} it makes sense to have both defined in the same file - but this is
   * not required.
   */
  public static class Handler extends DomainQueryHandler.Many<GetAllUsers, UsersRecord> {
    public Handler() {
      super(GetAllUsers.class);
    }

    @Override
    protected List<UsersRecord> run(GetAllUsers query, DSLContext dsl) {
      return dsl.selectFrom(USERS).fetchStream().toList();
    }
  }
}

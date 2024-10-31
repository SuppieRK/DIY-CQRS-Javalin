package io.github.suppierk.ddd.javalin.users.queries;

import static io.github.suppierk.example.Tables.USERS;

import io.github.suppierk.ddd.cqrs.DomainMessage;
import io.github.suppierk.ddd.cqrs.DomainQuery;
import io.github.suppierk.ddd.cqrs.DomainQueryHandler;
import io.github.suppierk.example.tables.records.UsersRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Domain query to fetch existing user from the system.
 *
 * <p>This is just an example of {@link DomainQuery} definition.
 *
 * <p>Note that extending {@link DomainQuery.One} interface for record works nicely with Java {@link
 * Record}s.
 *
 * @param messageId to identify this command
 * @param createdAt is the time when this command was requested
 * @param id of the existing user
 */
public record GetUser(UUID messageId, Instant createdAt, UUID id)
    implements DomainQuery.One<UUID, Instant> {
  /**
   * Alternative constructor, which automatically generates some of the {@link DomainMessage}
   * options.
   *
   * @param id of the existing user to fetch
   */
  public GetUser(UUID id) {
    this(UUID.randomUUID(), Instant.now(), id);
  }

  /** Post construct property validation. */
  public GetUser {
    if (id == null) {
      throw new IllegalArgumentException("Id cannot be null");
    }
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
  public static class Handler extends DomainQueryHandler.One<GetUser, UsersRecord> {
    public Handler() {
      super(GetUser.class);
    }

    /** {@inheritDoc} */
    @Override
    protected Optional<UsersRecord> run(GetUser query, DSLContext dsl) {
      return dsl.selectFrom(USERS).where(USERS.ID.eq(query.id)).fetchOptional();
    }
  }
}

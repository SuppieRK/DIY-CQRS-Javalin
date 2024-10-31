package io.github.suppierk.ddd.javalin.users;

import io.github.suppierk.ddd.async.DomainNotificationProducer;
import io.github.suppierk.ddd.cqrs.BoundedContext;
import io.github.suppierk.ddd.javalin.users.commands.CreateUser;
import io.github.suppierk.ddd.javalin.users.commands.DeleteUser;
import io.github.suppierk.ddd.javalin.users.commands.UpdateUser;
import io.github.suppierk.ddd.javalin.users.dto.User;
import io.github.suppierk.ddd.javalin.users.queries.GetAllUsers;
import io.github.suppierk.ddd.javalin.users.queries.GetUser;
import io.github.suppierk.ddd.jooq.DslContextProvider;
import io.github.suppierk.example.tables.Users;
import io.github.suppierk.example.tables.records.UsersRecord;
import io.javalin.http.Handler;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Represents {@link BoundedContext} for {@link UsersRecord}.
 *
 * <p>In addition, implements {@link UsersRestResource} to be used in REST API definition.
 */
public final class UsersBoundedContext extends BoundedContext<UsersRecord>
    implements UsersRestResource {
  public UsersBoundedContext(
      DslContextProvider readWriteDslContextProvider,
      DslContextProvider readOnlyDslContextProvider,
      DomainNotificationProducer domainNotificationProducer) {
    super(
        Users.USERS,
        readWriteDslContextProvider,
        readOnlyDslContextProvider,
        domainNotificationProducer);

    // Single entity operations
    addDomainCommandHandler(new CreateUser.Handler());
    addDomainQueryHandler(new GetUser.Handler());
    addDomainQueryHandler(new GetAllUsers.Handler());
    addDomainCommandHandler(new UpdateUser.Handler());
    addDomainCommandHandler(new DeleteUser.Handler());
  }

  @Override
  public Handler createUser() {
    return ctx -> {
      final var body = ctx.bodyAsClass(CreateUser.CreateUserRequest.class);
      final var command = new CreateUser(body);
      final var databaseRecord = createModel(command);
      final var json = new User(databaseRecord);
      ctx.json(json);
    };
  }

  @Override
  public Handler getAllUsers() {
    return ctx -> {
      final var command = new GetAllUsers();
      final var databaseRecords = queryManyModels(command);
      ctx.json(databaseRecords.stream().map(User::new).toList());
    };
  }

  @Override
  public Handler getUser(final UUID userId) {
    return ctx -> {
      final var command = new GetUser(userId);
      final var databaseRecord = queryOneModel(command);

      if (databaseRecord.isPresent()) {
        final var existingRecord = databaseRecord.get();
        final var json = new User(existingRecord);
        ctx.json(json);
      } else {
        ctx.res()
            .sendError(
                HttpServletResponse.SC_NOT_FOUND, "Cannot find user with ID: %s".formatted(userId));
      }
    };
  }

  @Override
  public Handler updateUser(final UUID userId) {
    return ctx -> {
      final var body = ctx.bodyAsClass(UpdateUser.UpdateUserRequest.class);
      final var command = new UpdateUser(userId, body);
      final var databaseRecord = updateModel(command);

      if (databaseRecord.isPresent()) {
        final var existingRecord = databaseRecord.get();
        final var json = new User(existingRecord);
        ctx.json(json);
      } else {
        ctx.res()
            .sendError(
                HttpServletResponse.SC_NOT_FOUND,
                "Cannot find resource with ID: %s".formatted(userId));
      }
    };
  }

  @Override
  public Handler deleteUser(final UUID userId) {
    return ctx -> {
      final var command = new DeleteUser(userId);
      deleteModel(command);
    };
  }
}

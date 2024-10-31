package io.github.suppierk.ddd.javalin;

import static io.github.suppierk.example.tables.Users.USERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.suppierk.ddd.javalin.configurations.Configuration;
import io.github.suppierk.ddd.javalin.users.commands.CreateUser;
import io.github.suppierk.ddd.javalin.users.commands.UpdateUser;
import io.github.suppierk.ddd.javalin.users.dto.User;
import io.github.suppierk.test.AbstractDatabaseTest;
import io.javalin.testtools.JavalinTest;
import java.util.Objects;
import java.util.Optional;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.source.ConfigSourcePackage;
import org.github.gestalt.config.source.MapConfigSourceBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationTest extends AbstractDatabaseTest {
  static final ObjectMapper MAPPER = new ObjectMapper();

  Application application;

  @BeforeEach
  void setUp() throws Exception {
    final ConfigSourcePackage configSourcePackage =
        MapConfigSourceBuilder.builder()
            .addCustomConfig("database.read-write.url", getJdbcUrl())
            .addCustomConfig("database.read-write.username", "test_rw_user")
            .addCustomConfig("database.read-write.password", "test_rw_password")
            .addCustomConfig("database.read-only.url", getJdbcUrl())
            .addCustomConfig("database.read-only.username", "test_ro_user")
            .addCustomConfig("database.read-only.password", "test_ro_password")
            .build();

    final Gestalt gestalt = new GestaltBuilder().addSource(configSourcePackage).build();

    gestalt.loadConfigs();

    application = new Application(Configuration.with(gestalt));
  }

  @AfterEach
  void tearDown() {
    truncate(USERS);
    application.close();
  }

  @Test
  void createUserHappyPath() {
    JavalinTest.test(
        application.javalin,
        (server, client) -> {
          final var request =
              new CreateUser.CreateUserRequest(
                  "user" + System.currentTimeMillis(),
                  "password" + System.currentTimeMillis(),
                  "email" + System.currentTimeMillis() + "@email.com");

          try (final var createUserResponse = client.post("/users", request)) {
            assertEquals(200, createUserResponse.code());

            final var createUserResponseBody =
                MAPPER.readValue(
                    Objects.requireNonNull(createUserResponse.body()).string(), User.class);
            assertNotNull(createUserResponseBody.id());
            assertEquals(request.username(), createUserResponseBody.username());
            assertEquals(request.email(), createUserResponseBody.email());

            assertEquals(1, count(USERS, USERS.ID.eq(createUserResponseBody.id())));
          }
        });
  }

  @Test
  void getUserHappyPath() {
    JavalinTest.test(
        application.javalin,
        (server, client) -> {
          final var request =
              new CreateUser.CreateUserRequest(
                  "user" + System.currentTimeMillis(),
                  "password" + System.currentTimeMillis(),
                  "email" + System.currentTimeMillis() + "@email.com");

          try (final var createUserResponse = client.post("/users", request)) {
            assertEquals(200, createUserResponse.code());

            final var createUserResponseBody =
                MAPPER.readValue(
                    Objects.requireNonNull(createUserResponse.body()).string(), User.class);
            assertEquals(1, count(USERS, USERS.ID.eq(createUserResponseBody.id())));

            try (final var getUserResponse =
                client.get("/users/%s".formatted(createUserResponseBody.id()))) {
              assertEquals(200, getUserResponse.code());

              final var readUserResponseBody =
                  MAPPER.readValue(
                      Objects.requireNonNull(getUserResponse.body()).string(), User.class);

              assertEquals(readUserResponseBody.id(), createUserResponseBody.id());
              assertEquals(readUserResponseBody.username(), createUserResponseBody.username());
              assertEquals(readUserResponseBody.email(), createUserResponseBody.email());
            }
          }
        });
  }

  @Test
  void getAllUsersHappyPath() {
    JavalinTest.test(
        application.javalin,
        (server, client) -> {
          final var request =
              new CreateUser.CreateUserRequest(
                  "user" + System.currentTimeMillis(),
                  "password" + System.currentTimeMillis(),
                  "email" + System.currentTimeMillis() + "@email.com");

          try (final var createUserResponse = client.post("/users", request)) {
            assertEquals(200, createUserResponse.code());

            final var createUserResponseBody =
                MAPPER.readValue(
                    Objects.requireNonNull(createUserResponse.body()).string(), User.class);
            assertEquals(1, count(USERS, USERS.ID.eq(createUserResponseBody.id())));

            try (final var getAllUsersResponse = client.get("/users")) {
              assertEquals(200, getAllUsersResponse.code());

              final var readAllUsersResponseBody =
                  MAPPER.readValue(
                      Objects.requireNonNull(getAllUsersResponse.body()).string(), User[].class);

              assertEquals(1, readAllUsersResponseBody.length);
              assertEquals(createUserResponseBody.id(), readAllUsersResponseBody[0].id());
              assertEquals(
                  createUserResponseBody.username(), readAllUsersResponseBody[0].username());
              assertEquals(createUserResponseBody.email(), readAllUsersResponseBody[0].email());
            }
          }
        });
  }

  @Test
  void updateAccountTest() {
    final var mapper = new ObjectMapper();

    JavalinTest.test(
        application.javalin,
        (server, client) -> {
          final var request =
              new CreateUser.CreateUserRequest(
                  "user" + System.currentTimeMillis(),
                  "password" + System.currentTimeMillis(),
                  "email" + System.currentTimeMillis() + "@email.com");

          try (final var createUserResponse = client.post("/users", request)) {
            assertEquals(200, createUserResponse.code());

            final var createUserResponseBody =
                MAPPER.readValue(
                    Objects.requireNonNull(createUserResponse.body()).string(), User.class);
            assertEquals(1, count(USERS, USERS.ID.eq(createUserResponseBody.id())));

            final var databaseRecord =
                fetchOptional(USERS, USERS.ID.eq(createUserResponseBody.id()));
            assertTrue(databaseRecord.isPresent());
            final var databaseRecordVersion = databaseRecord.get().getVersion();

            final var updateRequest =
                new UpdateUser.UpdateUserRequest(
                    Optional.of("user" + System.currentTimeMillis()),
                    Optional.empty(),
                    Optional.empty());

            try (final var updateUserResponse =
                client.put("/users/%s".formatted(createUserResponseBody.id()), updateRequest)) {
              assertEquals(200, updateUserResponse.code());

              final var updateUserResponseBody =
                  MAPPER.readValue(
                      Objects.requireNonNull(updateUserResponse.body()).string(), User.class);

              assertEquals(updateUserResponseBody.id(), createUserResponseBody.id());
              assertEquals(updateUserResponseBody.username(), updateRequest.username().get());
              assertEquals(updateUserResponseBody.email(), createUserResponseBody.email());

              final var updatedRecord =
                  fetchOptional(USERS, USERS.ID.eq(createUserResponseBody.id()));
              assertTrue(updatedRecord.isPresent());
              assertEquals(updatedRecord.get().getVersion(), databaseRecordVersion + 1);
            }
          }
        });
  }

  @Test
  void deleteAccountTest() {
    JavalinTest.test(
        application.javalin,
        (server, client) -> {
          final var request =
              new CreateUser.CreateUserRequest(
                  "user" + System.currentTimeMillis(),
                  "password" + System.currentTimeMillis(),
                  "email" + System.currentTimeMillis() + "@email.com");

          try (final var createUserResponse = client.post("/users", request)) {
            assertEquals(200, createUserResponse.code());

            final var createUserResponseBody =
                MAPPER.readValue(
                    Objects.requireNonNull(createUserResponse.body()).string(), User.class);
            assertEquals(1, count(USERS, USERS.ID.eq(createUserResponseBody.id())));

            try (final var deleteUserResponse =
                client.delete("/users/%s".formatted(createUserResponseBody.id()))) {
              assertEquals(200, deleteUserResponse.code());

              assertEquals(0, count(USERS, USERS.ID.eq(createUserResponseBody.id())));
            }
          }
        });
  }
}

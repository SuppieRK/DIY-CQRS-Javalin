package io.github.suppierk.ddd.javalin.users;

import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;

import io.github.suppierk.ddd.cqrs.BoundedContext;
import io.github.suppierk.ddd.javalin.users.commands.CreateUser;
import io.github.suppierk.ddd.javalin.users.commands.UpdateUser;
import io.github.suppierk.ddd.javalin.users.dto.User;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import java.util.UUID;

/**
 * Represents REST resource endpoints with documentation separately from the business logic of
 * {@link BoundedContext}.
 *
 * @see <a href="https://en.wikipedia.org/wiki/REST">REST</a>
 */
public interface UsersRestResource extends EndpointGroup {
  /** Describing the structure of this REST resource. */
  @Override
  default void addEndpoints() {
    path(
        "/users",
        () -> {
          post(createUser());
          get(getAllUsers());

          path(
              "/{id}",
              () -> {
                get(ctx -> getUser(getUserId(ctx)).handle(ctx));
                put(ctx -> updateUser(getUserId(ctx)).handle(ctx));
                delete(ctx -> deleteUser(getUserId(ctx)).handle(ctx));
              });
        });
  }

  /**
   * Helper to retrieve the correct user ID parameter during implementation.
   *
   * <p>The reason to define this method here is because path parameter appears only here in API
   * definitions.
   *
   * <p>This could be extended further by providing more wrapping, parsing API request body, etc.
   * which should depend on the goals of the specific project and its complexity.
   *
   * @return user ID for the given request
   */
  default UUID getUserId(final Context ctx) {
    return UUID.fromString(ctx.pathParam("id"));
  }

  // ENDPOINTS AND RESPECTIVE OPEN API DOCUMENTATION BELOW

  @OpenApi(
      summary = "Create user",
      operationId = "createUser",
      path = "/users",
      methods = HttpMethod.POST,
      tags = {"Users"},
      requestBody =
          @OpenApiRequestBody(
              content = {@OpenApiContent(from = CreateUser.CreateUserRequest.class)}),
      responses = {@OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class))})
  Handler createUser();

  @OpenApi(
      summary = "Get all users",
      operationId = "getAllUsers",
      path = "/users",
      methods = HttpMethod.GET,
      tags = {"Users"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User[].class))
      })
  Handler getAllUsers();

  @OpenApi(
      summary = "Get user",
      operationId = "getUser",
      path = "/users/{id}",
      methods = HttpMethod.GET,
      pathParams = {
        @OpenApiParam(name = "id", type = UUID.class, description = "Account ID", required = true)
      },
      tags = {"Users"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "404")
      })
  Handler getUser(final UUID userId);

  @OpenApi(
      summary = "Update user",
      operationId = "updateUser",
      path = "/users/{id}",
      methods = HttpMethod.PUT,
      pathParams = {
        @OpenApiParam(name = "id", type = UUID.class, description = "Account ID", required = true)
      },
      requestBody =
          @OpenApiRequestBody(
              content = {@OpenApiContent(from = UpdateUser.UpdateUserRequest.class)}),
      tags = {"Users"},
      responses = {
        @OpenApiResponse(status = "200", content = @OpenApiContent(from = User.class)),
        @OpenApiResponse(status = "404")
      })
  Handler updateUser(final UUID userId);

  @OpenApi(
      summary = "Delete user",
      operationId = "deleteUser",
      path = "/users/{id}",
      methods = HttpMethod.DELETE,
      pathParams = {
        @OpenApiParam(name = "id", type = UUID.class, description = "Account ID", required = true)
      },
      tags = {"Users"},
      responses = {@OpenApiResponse(status = "200")})
  Handler deleteUser(final UUID userId);
}

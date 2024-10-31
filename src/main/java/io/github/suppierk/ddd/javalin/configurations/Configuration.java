package io.github.suppierk.ddd.javalin.configurations;

import java.util.Objects;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.ClassPathConfigSourceBuilder;

/**
 * Defines rules to parse configuration based on the Gestalt library.
 *
 * @see <a href="https://github.com/gestalt-config/gestalt">Gestalt GitHub repository</a>
 */
public final class Configuration {
  private final Gestalt config;

  private Configuration(final Gestalt config) {
    this.config = Objects.requireNonNull(config);
  }

  /**
   * @return default configuration backed by lazy initialized singleton
   */
  public static Configuration get() {
    return Holder.INSTANCE;
  }

  /**
   * @param config to be used
   * @return configuration overridden by the end user, for example, during tests
   */
  public static Configuration with(final Gestalt config) {
    return new Configuration(config);
  }

  /**
   * @return port this server should launch on
   * @throws GestaltException any errors such as if there are no configs.
   */
  public int serverPort() throws GestaltException {
    return config.getConfig("server.port", Integer.class);
  }

  /**
   * @return database properties with read-write capability
   * @throws GestaltException any errors such as if there are no configs.
   */
  public Database readWriteDatabaseConnection() throws GestaltException {
    return config.getConfig("database.read-write", Database.class);
  }

  /**
   * @return database properties with read-only capability
   * @throws GestaltException any errors such as if there are no configs.
   */
  public Database readOnlyDatabaseConnection() throws GestaltException {
    return config.getConfig("database.read-only", Database.class);
  }

  /**
   * Basic immutable wrapper for database connection properties.
   *
   * @param url of the database
   * @param username to use for database connection
   * @param password to use for database connection
   */
  public record Database(String url, String username, String password) {
    /**
     * @return {@code true} if all properties are present, {@code false} otherwise
     */
    public boolean isPresent() {
      return url != null
          && !url.isBlank()
          && username != null
          && !username.isBlank()
          && password != null
          && !password.isBlank();
    }
  }

  /**
   * @see <a
   *     href="https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom">Initialization-on-demand
   *     holder idiom</a>
   */
  private static class Holder {
    private static final Configuration INSTANCE;

    static {
      try {
        final var defaultConfig =
            new GestaltBuilder()
                .addSource(
                    ClassPathConfigSourceBuilder.builder().setResource("application.yml").build())
                .build();

        defaultConfig.loadConfigs();

        INSTANCE = new Configuration(defaultConfig);
      } catch (GestaltException e) {
        throw new IllegalStateException("Failed to load application configuration", e);
      }
    }
  }
}

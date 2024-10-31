package io.github.suppierk.ddd.javalin;

import static io.javalin.apibuilder.ApiBuilder.*;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.suppierk.ddd.async.DomainNotificationProducer;
import io.github.suppierk.ddd.cqrs.BoundedContext;
import io.github.suppierk.ddd.javalin.configurations.Configuration;
import io.github.suppierk.ddd.javalin.users.UsersBoundedContext;
import io.github.suppierk.ddd.jooq.DslContextProvider;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import io.javalin.micrometer.MicrometerPlugin;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.db.MetricsDSLContext;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.DiskSpaceMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.io.Closeable;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.flywaydb.core.Flyway;
import org.github.gestalt.config.exceptions.GestaltException;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

/**
 * A sample application to be executed.
 *
 * <p>This class wraps up several important operations:
 *
 * <ul>
 *   <li>Leverage configuration to setup database connections.
 *   <li>Setup {@link BoundedContext}s and register their endpoints in {@link Javalin} server.
 *   <li>Kick off the application itself.
 * </ul>
 */
public final class Application implements Closeable {
  private static final Consumer<JavalinConfig> DISABLE_BANNER =
      config -> config.showJavalinBanner = false;

  final Javalin javalin;
  private final HikariDataSource readWriteDataSource;
  private final HikariDataSource readOnlyDataSource;

  private final DSLContext readWriteDsl;
  private final DSLContext readOnlyDsl;

  private final MeterRegistry meterRegistry;

  /** Default constructor. */
  Application(Configuration configuration) throws GestaltException {
    // Instrumentation
    this.meterRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

    // Database
    final var settings = new Settings();
    settings.setExecuteWithOptimisticLocking(true);

    // Setup read-write database connection
    final Configuration.Database rwConnection = configuration.readWriteDatabaseConnection();
    if (rwConnection.isPresent()) {
      this.readWriteDataSource = createDataSource(rwConnection);
    } else {
      throw new IllegalStateException("Can't find database read-write connection");
    }

    this.readWriteDsl =
        MetricsDSLContext.withMetrics(
            DSL.using(this.readWriteDataSource, SQLDialect.POSTGRES, settings),
            meterRegistry,
            Collections.singleton(Tag.of("connection.type", "read-write")));

    // Run migration scripts against container
    Flyway.configure().dataSource(readWriteDataSource).load().migrate();

    // Setup read-only connection
    final Configuration.Database roConnection = configuration.readOnlyDatabaseConnection();
    if (roConnection.isPresent()) {
      this.readOnlyDataSource = createDataSource(roConnection);
    } else {
      this.readOnlyDataSource = this.readWriteDataSource;
    }

    this.readOnlyDsl =
        MetricsDSLContext.withMetrics(
            DSL.using(this.readOnlyDataSource, SQLDialect.POSTGRES, settings),
            meterRegistry,
            Collections.singleton(Tag.of("connection.type", "read-only")));

    // Create the app itself
    this.javalin =
        Javalin.create(
            DISABLE_BANNER.andThen(
                javalinConfig -> {
                  // Add Micrometer
                  meterRegistry.config().commonTags("application", "cqrs-javalin");

                  new ClassLoaderMetrics().bindTo(meterRegistry);
                  new JvmMemoryMetrics().bindTo(meterRegistry);
                  new JvmGcMetrics().bindTo(meterRegistry);
                  new JvmThreadMetrics().bindTo(meterRegistry);
                  new UptimeMetrics().bindTo(meterRegistry);
                  new ProcessorMetrics().bindTo(meterRegistry);
                  new DiskSpaceMetrics(new File(System.getProperty("user.dir")))
                      .bindTo(meterRegistry);

                  final MicrometerPlugin micrometerPlugin =
                      new MicrometerPlugin(
                          micrometerPluginConfig ->
                              micrometerPluginConfig.registry = meterRegistry);
                  javalinConfig.registerPlugin(micrometerPlugin);

                  // Add Swagger functionality
                  javalinConfig.registerPlugin(
                      new OpenApiPlugin(
                          pluginConfig ->
                              pluginConfig.withDefinitionConfiguration(
                                  (version, definition) ->
                                      definition.withInfo(info -> info.setTitle("CQRS example")))));
                  javalinConfig.registerPlugin(new SwaggerPlugin());

                  // Configuring Jackson
                  javalinConfig.jsonMapper(
                      new JavalinJackson()
                          .updateMapper(mapper -> mapper.registerModule(new Jdk8Module())));

                  // Add service endpoints
                  javalinConfig.router.apiBuilder(
                      () -> {
                        // Healthcheck endpoint
                        get("/", ctx -> ctx.result("Up and running!"));

                        // Metrics endpoint
                        get(
                            "/metrics",
                            ctx -> {
                              final String contentType = "text/plain; version=0.0.4; charset=utf-8";
                              ctx.contentType(contentType)
                                  .result(((PrometheusMeterRegistry) meterRegistry).scrape());
                            });
                      });

                  // Add bounded contexts
                  for (EndpointGroup endpointGroup : boundedContexts()) {
                    javalinConfig.router.apiBuilder(endpointGroup);
                  }
                }));
  }

  /**
   * Shortcut to define all {@link BoundedContext}s in one place.
   *
   * @return a list of this app {@link BoundedContext}s
   */
  private Collection<? extends EndpointGroup> boundedContexts() {
    final var readWriteDslProvider = DslContextProvider.dslContextIdentity(readWriteDsl);
    final var readOnlyDslProvider = DslContextProvider.dslContextIdentity(readOnlyDsl);

    return List.of(
        new UsersBoundedContext(
            readWriteDslProvider, readOnlyDslProvider, DomainNotificationProducer.empty()));
  }

  /**
   * Shortcut to create {@link HikariDataSource}.
   *
   * @param databaseConfiguration properties to be used for the data source
   * @return prepared {@link HikariDataSource}
   */
  private HikariDataSource createDataSource(Configuration.Database databaseConfiguration) {
    HikariConfig hikariReadOnlyConfig = new HikariConfig();
    hikariReadOnlyConfig.setDriverClassName(org.postgresql.Driver.class.getName());
    hikariReadOnlyConfig.setJdbcUrl(databaseConfiguration.url());
    hikariReadOnlyConfig.setUsername(databaseConfiguration.username());
    hikariReadOnlyConfig.setPassword(databaseConfiguration.password());
    return new HikariDataSource(hikariReadOnlyConfig);
  }

  /**
   * Starts Javalin on given port.
   *
   * @param port to use to start the app
   */
  public void start(int port) {
    javalin.start(port);
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    readWriteDataSource.close();
    readOnlyDataSource.close();
    javalin.stop();
  }

  /**
   * Application entry point.
   *
   * @param args to pass for parsing
   */
  @SuppressWarnings("squid:S2095")
  public static void main(String[] args) throws Exception {
    final var configuration = Configuration.get();
    final var app = new Application(configuration);

    // Registering JVM shutdown hook to close the app
    Runtime.getRuntime().addShutdownHook(new Thread(app::close));

    // Launching the app
    app.start(configuration.serverPort());
  }
}

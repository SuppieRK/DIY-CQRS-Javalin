package io.github.suppierk.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.time.Duration;
import java.util.Optional;
import org.awaitility.Awaitility;
import org.flywaydb.core.Flyway;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;

@SuppressWarnings("unused")
public abstract class AbstractDatabaseTest {
  private static final PostgreSQLContainer<?> POSTGRESQL =
      new PostgreSQLContainer<>("postgres:16-alpine");

  private static HikariDataSource dataSource;
  private static DSLContext dsl;

  @BeforeAll
  static void beforeAll() {
    POSTGRESQL.setCommand("postgres", "-c", "fsync=off", "-c", "log_statement=all");
    POSTGRESQL.start();

    Awaitility.await().atMost(Duration.ofSeconds(15)).until(POSTGRESQL::isRunning);

    // Run migration against container
    Flyway.configure()
        .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();

    // Attach DSL context for additional operations
    HikariConfig hikariReadWriteConfig = new HikariConfig();
    hikariReadWriteConfig.setDriverClassName(POSTGRESQL.getDriverClassName());
    hikariReadWriteConfig.setJdbcUrl(POSTGRESQL.getJdbcUrl());
    hikariReadWriteConfig.setUsername(POSTGRESQL.getUsername());
    hikariReadWriteConfig.setPassword(POSTGRESQL.getPassword());
    dataSource = new HikariDataSource(hikariReadWriteConfig);

    dsl = DSL.using(dataSource, SQLDialect.POSTGRES);
  }

  @AfterAll
  static void afterAll() {
    dataSource.close();
    POSTGRESQL.stop();
  }

  protected final String getDriverClassName() {
    return POSTGRESQL.getDriverClassName();
  }

  protected final String getJdbcUrl() {
    return POSTGRESQL.getJdbcUrl();
  }

  protected final String getDatabaseName() {
    return POSTGRESQL.getDatabaseName();
  }

  protected final String getUsername() {
    return POSTGRESQL.getUsername();
  }

  protected final String getPassword() {
    return POSTGRESQL.getPassword();
  }

  protected final String getTestQueryString() {
    return POSTGRESQL.getTestQueryString();
  }

  protected final void truncate(Table<?>... tables) {
    dsl.truncate(tables).cascade().execute();
  }

  protected final int count(Table<?> table, Condition condition) {
    return dsl.selectCount().from(table).where(condition).fetchOne(0, int.class);
  }

  protected final <R extends UpdatableRecord<R>> Optional<R> fetchOptional(
      Table<R> table, Condition condition) {
    return dsl.selectFrom(table).where(condition).fetchOptional();
  }
}

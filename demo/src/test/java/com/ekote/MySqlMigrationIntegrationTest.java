package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "EKOTE_MIGRATION_TEST_URL", matches = "jdbc:mysql:.*")
class MySqlMigrationIntegrationTest {
  DriverManagerDataSource datasource;
  Flyway flyway;
  boolean isolatedValidated;

  @BeforeEach
  void setup() {
    datasource =
        new DriverManagerDataSource(
            System.getenv("EKOTE_MIGRATION_TEST_URL"),
            System.getenv("SPRING_DATASOURCE_USERNAME"),
            System.getenv("SPRING_DATASOURCE_PASSWORD"));
    flyway =
        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migration/mysql")
            .cleanDisabled(false)
            .load();
    // This suite accepts only its explicitly named disposable database.
    assertTrue(
        System.getenv("EKOTE_MIGRATION_TEST_URL")
            .matches("jdbc:mysql://[^/]+/ekote_migration_test(?:\\?.*)?"));
    isolatedValidated = true;
    flyway.clean();
  }

  @AfterEach
  void cleanup() {
    if (isolatedValidated) flyway.clean();
  }

  @Test
  void migratesEmptySchemaAndRevalidatesWithoutRepeatingChanges() {
    assertEquals(3, flyway.migrate().migrationsExecuted);
    flyway.validate();
    assertEquals(0, flyway.migrate().migrationsExecuted);
    JdbcTemplate sql = new JdbcTemplate(datasource);
    assertEquals(0, sql.queryForObject("SELECT COUNT(*) FROM guns", Integer.class));
    assertEquals(0, sql.queryForObject("SELECT COUNT(*) FROM SPRING_SESSION", Integer.class));
    sql.update(
        "INSERT INTO register(id,mail,password,role) VALUES (1,'owner@example.test','original-hash','ADMIN')");
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            sql.update(
                "INSERT INTO register(id,mail,password,role) VALUES (2,'owner@example.test','replacement','USER')"));
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            sql.update(
                "INSERT INTO register(id,mail,password,role) VALUES (1,'other@example.test','replacement','USER')"));
    assertEquals(
        "original-hash",
        sql.queryForObject("SELECT password FROM register WHERE id=1", String.class));
    assertEquals("ADMIN", sql.queryForObject("SELECT role FROM register WHERE id=1", String.class));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> sql.update("INSERT INTO guns(gun_name,model,quantity) VALUES ('Rifle','M',NULL)"));
  }

  @Test
  void refusesUnknownNonemptySchemaUntilExplicitlyBaselined() {
    JdbcTemplate sql = new JdbcTemplate(datasource);
    sql.execute("CREATE TABLE legacy_marker (id INT PRIMARY KEY)");
    assertThrows(FlywayException.class, () -> flyway.migrate());
    sql.execute("DROP TABLE legacy_marker");
    Flyway first =
        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migration/mysql")
            .target("1")
            .load();
    first.migrate();
    sql.update("INSERT INTO guns(gun_name,model,quantity) VALUES ('Retained','V1',7)");
    sql.execute("DROP TABLE flyway_schema_history");
    assertThrows(FlywayException.class, () -> flyway.migrate());
    Flyway adopted =
        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migration/mysql")
            .baselineVersion("1")
            .load();
    adopted.baseline();
    assertEquals(2, adopted.migrate().migrationsExecuted);
    assertEquals(
        7, sql.queryForObject("SELECT quantity FROM guns WHERE model='V1'", Integer.class));
  }

  @Test
  @SuppressWarnings({"unchecked", "rawtypes"})
  void independentSessionRepositoriesShareAuthenticationStateAndDeletion() {
    flyway.migrate();
    var sql = new JdbcTemplate(datasource);
    var tx = new TransactionTemplate(new JdbcTransactionManager(datasource));
    org.springframework.session.SessionRepository<org.springframework.session.Session> nodeA =
        (org.springframework.session.SessionRepository) new JdbcIndexedSessionRepository(sql, tx);
    org.springframework.session.SessionRepository<org.springframework.session.Session> nodeB =
        (org.springframework.session.SessionRepository) new JdbcIndexedSessionRepository(sql, tx);
    var session = nodeA.createSession();
    session.setAttribute("verified-user", "member@example.test");
    nodeA.save(session);
    var loaded = nodeB.findById(session.getId());
    assertNotNull(loaded);
    assertEquals("member@example.test", loaded.getAttribute("verified-user"));
    loaded.setAttribute("csrf-state", "shared");
    nodeB.save(loaded);
    assertEquals("shared", nodeA.findById(session.getId()).getAttribute("csrf-state"));
    nodeB.deleteById(session.getId());
    assertNull(nodeA.findById(session.getId()));
  }
}

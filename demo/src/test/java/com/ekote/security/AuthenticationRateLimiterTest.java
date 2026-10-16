package com.ekote.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AuthenticationRateLimiterTest {
  JdbcTemplate jdbc;
  Clock now = Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setup() {
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                "jdbc:h2:mem:throttle-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
    jdbc.execute(
        "CREATE TABLE authentication_rate_limits(bucket_key VARCHAR(64) PRIMARY KEY, window_started_at BIGINT NOT NULL, attempts INT NOT NULL)");
  }

  @Test
  void twoInstancesCannotExceedSharedLimitUnderConcurrency() throws Exception {
    var a = new AuthenticationRateLimiter(jdbc, now);
    var b = new AuthenticationRateLimiter(jdbc, now);
    var calls = new ArrayList<Callable<Boolean>>();
    for (int i = 0; i < 40; i++) {
      var instance = i % 2 == 0 ? a : b;
      calls.add(() -> instance.allow("192.0.2.1", "/login"));
    }
    try (var executor = Executors.newFixedThreadPool(8)) {
      int allowed = 0;
      for (var future : executor.invokeAll(calls)) if (future.get()) allowed++;
      assertEquals(20, allowed);
    }
    assertEquals(
        20, jdbc.queryForObject("SELECT attempts FROM authentication_rate_limits", Integer.class));
  }

  @Test
  void windowsRoutesAndNetworkAddressesRemainIndependent() {
    var limiter = new AuthenticationRateLimiter(jdbc, now);
    for (int i = 0; i < 5; i++) assertTrue(limiter.allow("192.0.2.1", "/register"));
    assertFalse(limiter.allow("192.0.2.1", "/register"));
    assertTrue(limiter.allow("192.0.2.1", "/login"));
    assertTrue(limiter.allow("192.0.2.2", "/register"));
    assertFalse(
        new AuthenticationRateLimiter(jdbc, Clock.offset(now, java.time.Duration.ofMinutes(9)))
            .allow("192.0.2.1", "/register"));
    assertTrue(
        new AuthenticationRateLimiter(jdbc, Clock.offset(now, java.time.Duration.ofMinutes(10)))
            .allow("192.0.2.1", "/register"));
    for (String key :
        jdbc.queryForList("SELECT bucket_key FROM authentication_rate_limits", String.class))
      assertTrue(key.matches("[a-f0-9]{64}"));
  }

  @Test
  void cleanupRetainsRecentBucketsAndBoundsExpiredWork() {
    for (int i = 0; i < 1001; i++)
      jdbc.update(
          "INSERT INTO authentication_rate_limits VALUES (?,?,1)",
          "old-" + i,
          now.millis() - 7200000);
    var limiter = new AuthenticationRateLimiter(jdbc, now);
    assertTrue(limiter.allow("192.0.2.1", "/login"));
    limiter.removeExpiredBuckets();
    assertEquals(
        2, jdbc.queryForObject("SELECT COUNT(*) FROM authentication_rate_limits", Integer.class));
    limiter.removeExpiredBuckets();
    assertEquals(
        1, jdbc.queryForObject("SELECT COUNT(*) FROM authentication_rate_limits", Integer.class));
    assertTrue(limiter.allow("192.0.2.1", "/login"));
  }
}

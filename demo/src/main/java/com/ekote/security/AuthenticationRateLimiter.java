package com.ekote.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Atomic database buckets protect every replica without locking a victim's account. */
@Component
public class AuthenticationRateLimiter {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public AuthenticationRateLimiter(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  public boolean allow(String address, String route) {
    int limit = route.equals("/login") ? 20 : 5;
    long window = Duration.ofMinutes(route.equals("/login") ? 1 : 10).toMillis();
    long now = clock.millis();
    String key = hash(route + "\n" + address);
    if (increment(key, now, window, limit)) return true;
    try {
      jdbc.update(
          "INSERT INTO authentication_rate_limits(bucket_key,window_started_at,attempts) VALUES (?,?,1)",
          key,
          now);
      return true;
    } catch (DuplicateKeyException concurrentOrFull) {
      // A different process may have inserted the first attempt. Retry atomically.
      return increment(key, now, window, limit);
    }
  }

  private boolean increment(String key, long now, long window, int limit) {
    long cutoff = now - window;
    return jdbc.update(
            "UPDATE authentication_rate_limits SET attempts = CASE WHEN window_started_at <= ? THEN 1 ELSE attempts + 1 END, window_started_at = CASE WHEN window_started_at <= ? THEN ? ELSE window_started_at END WHERE bucket_key = ? AND (window_started_at <= ? OR attempts < ?)",
            cutoff,
            cutoff,
            now,
            key,
            cutoff,
            limit)
        == 1;
  }

  @Scheduled(fixedDelay = 600000, initialDelay = 600000)
  public void removeExpiredBuckets() {
    long cutoff = clock.millis() - Duration.ofHours(1).toMillis();
    // Bound work per sweep and retain buckets refreshed concurrently by another replica.
    for (String key :
        jdbc.queryForList(
            "SELECT bucket_key FROM authentication_rate_limits WHERE window_started_at < ? LIMIT 1000",
            String.class,
            cutoff)) {
      jdbc.update(
          "DELETE FROM authentication_rate_limits WHERE bucket_key = ? AND window_started_at < ?",
          key,
          cutoff);
    }
  }

  private static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}

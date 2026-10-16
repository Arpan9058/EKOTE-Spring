package com.ekote.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Mapping for Hibernate validation and isolated H2 tests; updates use atomic JDBC. */
@Entity
@Table(name = "authentication_rate_limits")
public class AuthenticationRateLimit {
  @Id
  @Column(name = "bucket_key", length = 64, nullable = false)
  private String bucketKey;

  @Column(name = "window_started_at", nullable = false)
  private long windowStartedAt;

  @Column(nullable = false)
  private int attempts;
}

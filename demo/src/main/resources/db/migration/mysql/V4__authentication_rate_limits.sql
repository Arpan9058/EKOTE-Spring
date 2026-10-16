-- Shared across app replicas; stores hashed network/route buckets, never passwords.
CREATE TABLE authentication_rate_limits (
  bucket_key varchar(64) NOT NULL,
  window_started_at bigint NOT NULL,
  attempts int NOT NULL,
  PRIMARY KEY (bucket_key),
  KEY idx_auth_limit_expiry (window_started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

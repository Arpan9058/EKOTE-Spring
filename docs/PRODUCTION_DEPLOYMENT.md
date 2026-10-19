# Production deployment inputs and verification

The repository's kind/MySQL setup is an isolated integration lab. It is not a
multi-zone managed deployment. Select a hosting provider, region, DNS name and
database endpoint before provisioning live resources or scheduling offsite backups.

## Application and TLS

Set `SPRING_PROFILES_ACTIVE=production` and an explicit HTTPS
`EKOTE_PUBLIC_BASE_URL`. Startup rejects unsafe public origins or weakened session
cookies. The production profile emits structured JSON logs with request IDs.

Either terminate TLS at a configured private ingress/load balancer or use Spring
Boot's embedded HTTPS connector with a provider-issued PKCS12 certificate:

```sh
export SERVER_SSL_ENABLED=true
export SERVER_SSL_KEY_STORE=file:/run/secrets/server.p12
export SERVER_SSL_KEY_STORE_TYPE=PKCS12
# Supply SERVER_SSL_KEY_STORE_PASSWORD through the hosting secret manager.
```

The key store must contain the certificate chain and a private key for the actual
DNS name. Mount it read-only; renew certificates through the chosen provider.
The HTTPS regression test generates a disposable localhost-only identity and
verifies both certificate trust and hostname. It does not disable TLS verification
and does not establish that a live public endpoint has a valid certificate.

For a TLS edge, restrict the app's listener to that edge. Forwarded headers remain
disabled by default. Do not enable native proxy handling until the edge strips
client-supplied forwarding headers and Tomcat's trusted proxy addresses are
configured explicitly. The shared authentication quota uses the observed client
address, so verify client-IP propagation and spoof resistance on the deployed edge.

## Database availability

Use a managed MySQL 8.4 service with automatic failover across failure domains and
private network access. Use a provider DNS endpoint and Connector/J
`sslMode=VERIFY_IDENTITY`, with the provider CA in the JVM trust store. Supply the
database username/password via a secret manager, separately from the JDBC URL.
Test failover, recovery and session continuity against that actual service.

Run application replicas on separate nodes/zones with enough capacity for a
rolling update. The local PodDisruptionBudget protects voluntary disruptions; it
does not create nodes, replicate MySQL or guarantee availability during an outage.
Select maintained Kubernetes/CNI releases for the provider rather than deploying
the pinned compatibility lab as production infrastructure.

## Backups and operational ownership

The private streaming exporter and exact-row restore drill are documented in
[database operations](DATABASE_OPERATIONS.md). Configure the selected provider's
encrypted backup storage, an independent access role, retention/object locking,
schedules and failure alerts. Keep restoration keys and permissions available to
the recovery operator; test restoration from the offsite copy into a fresh database.

Provider account, region, bucket/vault, DNS ownership, certificate management and
retention/RPO/RTO requirements are deployment inputs. CI verifies the application,
local recovery and policy enforcement; it cannot provision or certify an external
HA database or offsite backup destination without those inputs and access.

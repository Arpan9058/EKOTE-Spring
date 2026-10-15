# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk@sha256:3e3c176ffed168beb42c607be9bc1639b466cf00261a0fb04425562c9d0c5c2b AS build
WORKDIR /app
COPY . .
# Optional BuildKit secrets support environments using a managed network proxy.
# Normal local builds need neither secret.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=java_truststore,target=/tmp/java-truststore \
    --mount=type=secret,id=ca_bundle,target=/etc/ssl/certs/ca-certificates.crt \
    export https_proxy="$HTTPS_PROXY" http_proxy="$HTTP_PROXY" no_proxy="$NO_PROXY"; \
    if [ -f /tmp/java-truststore ]; then export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/tmp/java-truststore"; fi; \
    chmod +x ./mvnw && ./mvnw --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre@sha256:ccbad9e7cb84c27092b12c40a4143e5733e050db9c3784d84f632dcf423fb2ef
# Security fixes newer than the pinned upstream runtime image. Keep exact versions reviewed.
RUN --mount=type=secret,id=ca_bundle,target=/etc/ssl/certs/ca-certificates.crt \
    set -eu; \
    export https_proxy="$HTTPS_PROXY" http_proxy="$HTTP_PROXY" no_proxy="$NO_PROXY"; \
    apt-get update; \
    apt-get install -y --only-upgrade --no-install-recommends \
      curl=8.18.0-1ubuntu2.8 libcurl4t64=8.18.0-1ubuntu2.8 \
      libfreetype6=2.14.2+dfsg-1ubuntu0.2 libpng16-16t64=1.6.57-1ubuntu0.1; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

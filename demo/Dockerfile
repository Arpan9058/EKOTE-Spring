# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk@sha256:23cc5734ff46e776f51ce66eb9ee08444fd47caabec0a95b7eb6e94be2d3696d AS build
# The wrapper's checksum pins the ZIP archive. Without unzip it switches to tar.gz,
# which is a different artifact and must never be accepted against the ZIP checksum.
RUN --mount=type=secret,id=ca_bundle,target=/etc/ssl/certs/ca-certificates.crt,mode=0444 \
    set -eu; \
    export https_proxy="${HTTPS_PROXY:-}" http_proxy="${HTTP_PROXY:-}" no_proxy="${NO_PROXY:-}"; \
    sed -i 's|http://|https://|g' /etc/apt/sources.list.d/ubuntu.sources; \
    apt-get update; \
    apt-get install -y --no-install-recommends unzip; \
    rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY . .
# Optional BuildKit secrets support environments using a managed network proxy.
# Normal local builds need neither secret.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml \
    --mount=type=secret,id=java_truststore,target=/tmp/java-truststore \
    --mount=type=secret,id=ca_bundle,target=/etc/ssl/certs/ca-certificates.crt \
    export https_proxy="${HTTPS_PROXY:-}" http_proxy="${HTTP_PROXY:-}" no_proxy="${NO_PROXY:-}"; \
    if [ -f /tmp/java-truststore ]; then export MAVEN_OPTS="-Djavax.net.ssl.trustStore=/tmp/java-truststore"; fi; \
    chmod +x ./mvnw && ./mvnw --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre-alpine@sha256:51ab5e3302e7141ce665ca3ea85e8b5cd648eafbc3c0c90dd79d6537684e4555
# Apply vendor fixes newer than the pinned image; keep package signatures enabled.
RUN --mount=type=secret,id=ca_bundle,target=/etc/ssl/certs/ca-certificates.crt \
    set -eu; \
    export https_proxy="${HTTPS_PROXY:-}" http_proxy="${HTTP_PROXY:-}" no_proxy="${NO_PROXY:-}"; \
    apk add --no-cache --upgrade libpng=1.6.59-r0 zlib=1.3.2-r1 libexpat=2.9.0-r0
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

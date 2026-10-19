# syntax=docker/dockerfile:1
FROM eclipse-temurin:24-jdk@sha256:7493205ffe6caa8074fa8a06a276bb1c5ac41d3dd0fd43a0db66d7f776e80b3e AS build
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

FROM eclipse-temurin:25-jre-alpine@sha256:3c0a9084927a221ccd1d007fcaf614465672c0af37aaa834c5184483afe56d61
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

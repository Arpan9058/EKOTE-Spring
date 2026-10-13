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

FROM eclipse-temurin:21-jre@sha256:cff19e6215689161eb6162c11b86b0c60ddf802164f2eaf48d570f8fb79a36c5
WORKDIR /app
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

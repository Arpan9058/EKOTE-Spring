package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import com.ekote.config.ProductionDeploymentGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProductionDeploymentGuardTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(context -> context.getEnvironment().setActiveProfiles("production"))
          .withUserConfiguration(ProductionDeploymentGuard.class)
          .withPropertyValues(
              "ekote.public-base-url=https://inventory.example.test",
              "server.servlet.session.cookie.secure=true",
              "server.servlet.session.cookie.http-only=true");

  @Test
  void validHttpsConfigurationStarts() {
    runner.run(context -> assertNull(context.getStartupFailure()));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://inventory.example.test",
        "https://user:password@inventory.example.test",
        "https://inventory.example.test?token=secret",
        "https://inventory.example.test/#fragment",
        "https://inventory.example.test:0",
        "https://inventory.example.test:70000",
        "not a URI"
      })
  void rejectsUnsafePublicOriginsWithoutEchoingTheirContents(String url) {
    runner
        .withPropertyValues("ekote.public-base-url=" + url)
        .run(
            context -> {
              assertNotNull(context.getStartupFailure());
              Throwable failure = context.getStartupFailure();
              while (failure.getCause() != null) failure = failure.getCause();
              assertEquals(
                  "Production public URL must be a valid HTTPS origin", failure.getMessage());
            });
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "server.servlet.session.cookie.secure=false",
        "server.servlet.session.cookie.http-only=false"
      })
  void rejectsWeakenedSessionCookies(String property) {
    runner.withPropertyValues(property).run(context -> assertNotNull(context.getStartupFailure()));
  }
}

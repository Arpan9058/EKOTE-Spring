package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import com.ekote.web.RequestIdFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class StructuredLoggingTest {
  @Test
  void productionConsoleEscapesMessagesAndCarriesOnlyTheCurrentRequestId() throws Exception {
    Path output = java.nio.file.Files.createTempFile("ekote-structured-log-", ".jsonl");
    try {
      var process =
          new ProcessBuilder(
                  Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                  "-cp",
                  System.getProperty(
                      "surefire.test.class.path", System.getProperty("java.class.path")),
                  LogProbe.class.getName())
              .redirectErrorStream(true)
              .redirectOutput(output.toFile())
              .start();
      if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        fail("Structured logging probe timed out");
      }
      String logs = java.nio.file.Files.readString(output, StandardCharsets.UTF_8);
      assertEquals(0, process.exitValue(), logs);
      var mapper = new JsonMapper();
      var records =
          logs.lines()
              .filter(line -> line.startsWith("{"))
              .map(mapper::readTree)
              .filter(node -> "ekote.logging.probe".equals(node.path("logger_name").asText()))
              .toList();
      assertEquals(2, records.size(), logs);
      var request = records.getFirst();
      assertEquals("request_123", request.path("requestId").asText());
      assertEquals("INFO", request.path("level").asText());
      assertFalse(request.path("@timestamp").asText().isBlank());
      assertEquals("quoted \"message\"\nsecond line", request.path("message").asText());
      assertFalse(records.get(1).has("requestId"), "Request MDC leaked after the filter returned");
    } finally {
      java.nio.file.Files.deleteIfExists(output);
    }
  }

  public static class LogProbe {
    public static void main(String[] args) throws Exception {
      var application = new SpringApplication(ProbeConfiguration.class);
      application.setWebApplicationType(WebApplicationType.NONE);
      try (var context =
          application.run("--spring.profiles.active=production", "--spring.main.banner-mode=off")) {
        var logger = LoggerFactory.getLogger("ekote.logging.probe");
        var request = new MockHttpServletRequest();
        request.addHeader("X-Request-ID", "request_123");
        new RequestIdFilter()
            .doFilter(
                request,
                new MockHttpServletResponse(),
                (req, res) -> logger.info("quoted \"message\"\nsecond line"));
        logger.info("after request");
      }
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class ProbeConfiguration {}
}

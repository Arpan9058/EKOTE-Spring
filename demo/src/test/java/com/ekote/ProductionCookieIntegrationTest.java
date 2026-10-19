package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "EKOTE_PUBLIC_BASE_URL=https://inventory.example.test")
@ActiveProfiles({"test", "production"})
class ProductionCookieIntegrationTest {
  private static Path keyStore;
  private static final String PASSWORD = UUID.randomUUID().toString();
  @LocalServerPort int port;

  @DynamicPropertySource
  static void configureTls(DynamicPropertyRegistry registry) throws Exception {
    keyStore = Files.createTempDirectory("ekote-test-tls-").resolve("server.p12");
    var builder =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair",
                "-alias",
                "server",
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-storetype",
                "PKCS12",
                "-keystore",
                keyStore.toString(),
                "-storepass:env",
                "EKOTE_TEST_TLS_PASSWORD",
                "-dname",
                "CN=localhost",
                "-ext",
                "SAN=dns:localhost,ip:127.0.0.1",
                "-validity",
                "1",
                "-noprompt")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD);
    builder.environment().put("EKOTE_TEST_TLS_PASSWORD", PASSWORD);
    var process = builder.start();
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("TLS test certificate generation timed out");
    }
    assertEquals(0, process.exitValue(), "TLS test certificate generation failed");
    registry.add("server.ssl.enabled", () -> true);
    registry.add("server.ssl.key-store", () -> keyStore.toUri().toString());
    registry.add("server.ssl.key-store-password", () -> PASSWORD);
    registry.add("server.ssl.key-store-type", () -> "PKCS12");
  }

  @AfterAll
  static void removeTestKey() throws Exception {
    if (keyStore != null) {
      Files.deleteIfExists(keyStore);
      Files.deleteIfExists(keyStore.getParent());
    }
  }

  @Test
  void verifiedHttpsConnectionIssuesSecureHttpOnlySameSiteSessionCookie() throws Exception {
    var identity = KeyStore.getInstance("PKCS12");
    try (var source = Files.newInputStream(keyStore)) {
      identity.load(source, PASSWORD.toCharArray());
    }
    var trust = KeyStore.getInstance(KeyStore.getDefaultType());
    trust.load(null, null);
    trust.setCertificateEntry("local-test", identity.getCertificate("server"));
    var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    managers.init(trust);
    var tls = SSLContext.getInstance("TLS");
    tls.init(null, managers.getTrustManagers(), null);
    var request =
        HttpRequest.newBuilder(URI.create("https://127.0.0.1:" + port + "/register")).GET().build();
    var response =
        HttpClient.newBuilder()
            .sslContext(tls)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
    assertTrue(response.sslSession().isPresent(), "Request must use a verified TLS connection");
    var cookie =
        response.headers().allValues("Set-Cookie").stream()
            .filter(value -> value.startsWith("SESSION="))
            .findFirst()
            .orElseThrow();
    assertTrue(cookie.contains("; Secure"), cookie);
    assertTrue(cookie.contains("; HttpOnly"), cookie);
    assertTrue(cookie.contains("SameSite=Lax"), cookie);
  }
}

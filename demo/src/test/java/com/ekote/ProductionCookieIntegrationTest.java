package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "EKOTE_PUBLIC_BASE_URL=https://inventory.example.test")
@ActiveProfiles({"test", "production"})
class ProductionCookieIntegrationTest {
  @LocalServerPort int port;

  @Test
  void registrationFormIssuesOnlySecureHttpOnlySameSiteSessionCookie() throws Exception {
    var request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/register")).GET().build();
    var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode());
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

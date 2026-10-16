package com.ekote.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.dao.DataAccessException;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationThrottleFilter extends OncePerRequestFilter {
  private final AuthenticationRateLimiter limiter;

  public AuthenticationThrottleFilter(AuthenticationRateLimiter limiter) {
    this.limiter = limiter;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    if (path.isEmpty()) {
      path =
          org.springframework.web.util.UriUtils.decode(
              request.getRequestURI().substring(request.getContextPath().length()),
              java.nio.charset.StandardCharsets.UTF_8);
    }
    if ("POST".equals(request.getMethod()) && (path.equals("/login") || path.equals("/register"))) {
      // Forwarded headers are not trusted here. Configure trusted proxy handling explicitly.
      try {
        if (!limiter.allow(request.getRemoteAddr(), path)) {
          reject(response, 429, path.equals("/login") ? 60 : 600);
          return;
        }
      } catch (DataAccessException unavailable) {
        reject(response, 503, 60);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private static void reject(HttpServletResponse response, int status, int retryAfter)
      throws IOException {
    response.setStatus(status);
    response.setHeader("Retry-After", String.valueOf(retryAfter));
    response.setHeader("Cache-Control", "no-store");
    response.setContentType("application/json");
    response
        .getWriter()
        .write(
            status == 429
                ? "{\"error\":\"Too many attempts; retry later\"}"
                : "{\"error\":\"Authentication temporarily unavailable\"}");
  }
}

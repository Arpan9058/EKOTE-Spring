package com.ekote.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestIdFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String id = request.getHeader("X-Request-ID");
    if (id == null || !id.matches("[A-Za-z0-9_-]{1,64}")) id = UUID.randomUUID().toString();
    response.setHeader("X-Request-ID", id);
    String previous = MDC.get("requestId");
    MDC.put("requestId", id);
    try {
      chain.doFilter(request, response);
    } finally {
      if (previous == null) MDC.remove("requestId");
      else MDC.put("requestId", previous);
    }
  }
}

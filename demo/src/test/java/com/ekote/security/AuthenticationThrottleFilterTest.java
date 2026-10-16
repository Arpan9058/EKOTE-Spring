package com.ekote.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationThrottleFilterTest {
  AuthenticationRateLimiter limiter = mock(AuthenticationRateLimiter.class);

  @Test
  void exhaustedBucketReturnsRetryAfterWithoutCallingAuthentication() throws Exception {
    var request = new MockHttpServletRequest("POST", "/login");
    request.setRemoteAddr("192.0.2.1");
    request.addHeader("X-Forwarded-For", "spoofed-address");
    var response = new MockHttpServletResponse();
    var chain = new MockFilterChain();
    new AuthenticationThrottleFilter(limiter).doFilter(request, response, chain);
    assertEquals(429, response.getStatus());
    assertEquals("60", response.getHeader("Retry-After"));
    assertEquals("no-store", response.getHeader("Cache-Control"));
    verify(limiter).allow("192.0.2.1", "/login");
    assertNull(chain.getRequest());
  }

  @Test
  void encodedLoginPathCannotBypassLimit() throws Exception {
    var response = new MockHttpServletResponse();
    new AuthenticationThrottleFilter(limiter)
        .doFilter(new MockHttpServletRequest("POST", "/%6cogin"), response, new MockFilterChain());
    assertEquals(429, response.getStatus());
    verify(limiter).allow(anyString(), eq("/login"));
  }

  @Test
  void databaseOutageFailsClosedAndDoesNotLeakDetails() throws Exception {
    when(limiter.allow(anyString(), anyString()))
        .thenThrow(new DataAccessResourceFailureException("private database detail"));
    var response = new MockHttpServletResponse();
    new AuthenticationThrottleFilter(limiter)
        .doFilter(new MockHttpServletRequest("POST", "/register"), response, new MockFilterChain());
    assertEquals(503, response.getStatus());
    assertFalse(response.getContentAsString().contains("private"));
  }

  @Test
  void ordinaryRequestsBypassQuotaAndAllowedAttemptsContinue() throws Exception {
    var filter = new AuthenticationThrottleFilter(limiter);
    var chain = new MockFilterChain();
    filter.doFilter(
        new MockHttpServletRequest("GET", "/login"), new MockHttpServletResponse(), chain);
    assertNotNull(chain.getRequest());
    verifyNoInteractions(limiter);
    when(limiter.allow(anyString(), anyString())).thenReturn(true);
    chain = new MockFilterChain();
    filter.doFilter(
        new MockHttpServletRequest("POST", "/login"), new MockHttpServletResponse(), chain);
    assertNotNull(chain.getRequest());
  }
}

package com.ekote;

import static org.junit.jupiter.api.Assertions.*;

import com.ekote.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {
  @Test
  void preservesSafeIdentifiersAndRestoresThreadContextOnFailure() {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "request_123");
    var response = new MockHttpServletResponse();
    MDC.put("requestId", "outer");
    try {
      assertThrows(
          jakarta.servlet.ServletException.class,
          () ->
              new RequestIdFilter()
                  .doFilter(
                      request,
                      response,
                      (req, res) -> {
                        assertEquals("request_123", MDC.get("requestId"));
                        throw new jakarta.servlet.ServletException("failure");
                      }));
      assertEquals("request_123", response.getHeader("X-Request-ID"));
      assertEquals("outer", MDC.get("requestId"));
    } finally {
      MDC.clear();
    }
  }

  @Test
  void replacesUnsafeOrUnboundedInput() throws Exception {
    for (String value : new String[] {"bad\r\nInjected: value", "x".repeat(65), ""}) {
      var request = new MockHttpServletRequest();
      request.addHeader("X-Request-ID", value);
      var response = new MockHttpServletResponse();
      new RequestIdFilter().doFilter(request, response, (req, res) -> {});
      assertDoesNotThrow(() -> java.util.UUID.fromString(response.getHeader("X-Request-ID")));
      assertNull(MDC.get("requestId"));
    }
  }
}

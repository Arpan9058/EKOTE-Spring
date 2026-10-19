package com.ekote.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Fail closed when production URL or session security is accidentally weakened. */
@Component
@Profile("production")
public class ProductionDeploymentGuard {
  public ProductionDeploymentGuard(
      @Value("${ekote.public-base-url}") String publicUrl,
      @Value("${server.servlet.session.cookie.secure:false}") boolean secure,
      @Value("${server.servlet.session.cookie.http-only:false}") boolean httpOnly) {
    URI uri;
    try {
      uri = URI.create(publicUrl);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("Production public URL must be a valid HTTPS origin");
    }
    if (!"https".equalsIgnoreCase(uri.getScheme())
        || uri.getHost() == null
        || uri.getRawUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || uri.getPort() == 0
        || uri.getPort() > 65535) {
      throw new IllegalStateException("Production public URL must be a valid HTTPS origin");
    }
    if (!secure || !httpOnly) {
      throw new IllegalStateException("Production session cookies require Secure and HttpOnly");
    }
  }
}

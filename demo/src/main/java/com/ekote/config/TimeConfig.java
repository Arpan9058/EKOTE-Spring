package com.ekote.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@org.springframework.scheduling.annotation.EnableScheduling
@Configuration
public class TimeConfig {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}

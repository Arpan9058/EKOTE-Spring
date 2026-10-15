package com.ekote.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InventoryMetrics {
  private final MeterRegistry registry;

  public InventoryMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void committed(StockChanged movement) {
    registry.counter("inventory.movements", "operation", movement.operation().name()).increment();
    registry
        .counter("inventory.units", "operation", movement.operation().name())
        .increment(Math.abs((long) movement.delta()));
  }
}

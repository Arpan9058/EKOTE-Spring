package com.ekote.support;

import static org.mockito.Mockito.mock;

import com.ekote.repositories.GunRepository;
import com.ekote.repositories.StockMovementRepository;
import com.ekote.service.InventoryStockService;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;

public final class StockFixtures {
  private StockFixtures() {}

  public static InventoryStockService stock(GunRepository guns) {
    return new InventoryStockService(
        guns,
        mock(EntityManager.class),
        mock(StockMovementRepository.class),
        mock(ApplicationEventPublisher.class));
  }
}

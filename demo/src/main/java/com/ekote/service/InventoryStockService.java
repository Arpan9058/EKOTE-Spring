package com.ekote.service;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class InventoryStockService {
  private final GunRepository guns;
  private final EntityManager entities;
  private final StockMovementRepository movements;
  private final ApplicationEventPublisher events;

  public InventoryStockService(
      GunRepository guns,
      EntityManager entities,
      StockMovementRepository movements,
      ApplicationEventPublisher events) {
    this.guns = guns;
    this.entities = entities;
    this.movements = movements;
    this.events = events;
  }

  public Optional<Gun> lockGun(int id) {
    // Discard the identity-map copy before the locking query hydrates current stock.
    // refresh() after a lock may issue a plain SELECT, which reads an older MySQL
    // REPEATABLE READ snapshot even while this transaction owns the row lock.
    entities.flush(); // Preserve earlier mutations when multiple operations share one transaction.
    entities.detach(entities.getReference(Gun.class, id));
    return guns.findForUpdateById(id);
  }

  public void apply(Gun gun, int delta, MovementType operation, String identifier) {
    if (delta == 0
        || ((operation == MovementType.ADD || operation == MovementType.RETURN) != (delta > 0)))
      throw new IllegalArgumentException("Movement direction does not match operation");
    int before = gun.getQuantity();
    int after = Math.addExact(before, delta);
    if (after < 0) throw new IllegalArgumentException("Insufficient inventory");
    gun.setQuantity(after);
    guns.save(gun);
    record(gun, before, after, operation, identifier);
  }

  public void recordNew(Gun gun) {
    record(gun, 0, gun.getQuantity(), MovementType.ADD, null);
  }

  private void record(Gun gun, int before, int after, MovementType operation, String identifier) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    String actor = auth != null ? auth.getName() : "system";
    movements.save(new StockMovement(gun, before, after, operation, identifier, actor));
    events.publishEvent(new StockChanged(operation, after - before));
  }
}

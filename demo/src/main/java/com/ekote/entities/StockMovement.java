package com.ekote.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "stock_movements",
    indexes = {@Index(name = "idx_movement_gun_time", columnList = "gun_id,occurred_at")})
public class StockMovement {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "gun_id", nullable = false, updatable = false)
  private int gunId;

  @Column(nullable = false, updatable = false)
  private String gunName;

  @Column(nullable = false, updatable = false)
  private String model;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private MovementType operation;

  @Column(nullable = false, updatable = false)
  private int quantityBefore;

  @Column(nullable = false, updatable = false)
  private int quantityAfter;

  @Column(nullable = false, updatable = false)
  private int delta;

  @Column(updatable = false)
  private String itemIdentifier;

  @Column(nullable = false, updatable = false)
  private String actor;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  protected StockMovement() {}

  public StockMovement(
      Gun gun, int before, int after, MovementType operation, String item, String actor) {
    if (gun.getId() <= 0 || before < 0 || after < 0 || operation == null)
      throw new IllegalArgumentException("Invalid stock movement");
    this.gunId = gun.getId();
    this.gunName = gun.getGunName();
    this.model = gun.getModel();
    this.quantityBefore = before;
    this.quantityAfter = after;
    this.delta = Math.subtractExact(after, before);
    this.operation = operation;
    this.itemIdentifier = item;
    this.actor = actor;
    this.occurredAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public int getGunId() {
    return gunId;
  }

  public String getGunName() {
    return gunName;
  }

  public String getModel() {
    return model;
  }

  public MovementType getOperation() {
    return operation;
  }

  public int getQuantityBefore() {
    return quantityBefore;
  }

  public int getQuantityAfter() {
    return quantityAfter;
  }

  public int getDelta() {
    return delta;
  }

  public String getItemIdentifier() {
    return itemIdentifier;
  }

  public String getActor() {
    return actor;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}

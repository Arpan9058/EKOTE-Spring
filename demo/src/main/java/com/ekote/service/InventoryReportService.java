package com.ekote.service;

import com.ekote.entities.*;
import com.ekote.repositories.*;
import java.time.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryReportService {
  private final GunRepository guns;
  private final GunInfoRepository items;
  private final IssueRepository issues;
  private final StockMovementRepository movements;
  private final Clock clock;

  public InventoryReportService(
      GunRepository guns,
      GunInfoRepository items,
      IssueRepository issues,
      StockMovementRepository movements,
      Clock clock) {
    this.guns = guns;
    this.items = items;
    this.issues = issues;
    this.movements = movements;
    this.clock = clock;
  }

  public record Summary(long models, long availableUnits, long activeIssues, long maintenanceDue) {}

  public record Stock(int id, String name, String model, int quantity) {}

  public record Maintenance(
      String identifier,
      int gunId,
      LocalDate lastPerformed,
      LocalDate due,
      Integer intervalMonths) {}

  public record Movement(
      Long id,
      int gunId,
      String name,
      String model,
      MovementType operation,
      int before,
      int after,
      int delta,
      String identifier,
      String actor,
      Instant occurredAt) {
    static Movement from(StockMovement m) {
      return new Movement(
          m.getId(),
          m.getGunId(),
          m.getGunName(),
          m.getModel(),
          m.getOperation(),
          m.getQuantityBefore(),
          m.getQuantityAfter(),
          m.getDelta(),
          m.getItemIdentifier(),
          m.getActor(),
          m.getOccurredAt());
    }
  }

  public record Result<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    static <T> Result<T> from(Page<T> p) {
      return new Result<>(
          p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
  }

  public Summary summary() {
    return new Summary(
        guns.count(),
        guns.totalAvailable(),
        issues.countByStatusAndReturnDateIsNull("issued"),
        items.countByNextMaintenanceDateLessThanEqual(LocalDate.now(clock)));
  }

  public Result<Stock> lowStock(int threshold, int page, int size) {
    if (threshold < 0) throw new IllegalArgumentException("Threshold must be non-negative");
    return Result.from(
        guns.findByQuantityLessThanEqual(threshold, pageable(page, size, Sort.by("quantity", "id")))
            .map(g -> new Stock(g.getId(), g.getGunName(), g.getModel(), g.getQuantity())));
  }

  public Result<Maintenance> due(int page, int size) {
    return Result.from(
        items
            .findByNextMaintenanceDateLessThanEqual(
                LocalDate.now(clock),
                pageable(page, size, Sort.by("nextMaintenanceDate", "uniqueIdentifier")))
            .map(
                i ->
                    new Maintenance(
                        i.getUniqueIdentifier(),
                        i.getGun().getId(),
                        i.getMaintenanceDate(),
                        i.getNextMaintenanceDate(),
                        i.getMaintenanceFreq())));
  }

  public Result<Movement> history(Integer gunId, Instant from, Instant to, int page, int size) {
    return Result.from(
        movements
            .findAll(
                filter(gunId, from, to),
                pageable(page, size, Sort.by(Sort.Direction.DESC, "occurredAt", "id")))
            .map(Movement::from));
  }

  public String export(Integer gunId, Instant from, Instant to) {
    var result =
        movements.findAll(
            filter(gunId, from, to), PageRequest.of(0, 5000, Sort.by("occurredAt", "id")));
    if (result.getTotalElements() > 5000)
      throw new IllegalArgumentException(
          "Export exceeds 5000 rows; narrow the date or model filter");
    StringBuilder csv =
        new StringBuilder(
            "id,gun_id,name,model,operation,before,after,delta,identifier,actor,occurred_at\r\n");
    for (StockMovement m : result) {
      csv.append(m.getId())
          .append(',')
          .append(m.getGunId())
          .append(',')
          .append(csvCell(m.getGunName()))
          .append(',')
          .append(csvCell(m.getModel()))
          .append(',')
          .append(m.getOperation())
          .append(',')
          .append(m.getQuantityBefore())
          .append(',')
          .append(m.getQuantityAfter())
          .append(',')
          .append(m.getDelta())
          .append(',')
          .append(csvCell(m.getItemIdentifier()))
          .append(',')
          .append(csvCell(m.getActor()))
          .append(',')
          .append(m.getOccurredAt())
          .append("\r\n");
    }
    return csv.toString();
  }

  static String csvCell(String value) {
    if (value == null) return "\"\"";
    String stripped = value.stripLeading();
    if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0
        || value.startsWith("\t")
        || value.startsWith("\r")
        || value.startsWith("\n")) value = "'" + value;
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }

  private static Pageable pageable(int page, int size, Sort sort) {
    if (page < 0 || size < 1 || size > 100)
      throw new IllegalArgumentException("Page must be non-negative and size between 1 and 100");
    return PageRequest.of(page, size, sort);
  }

  private static Specification<StockMovement> filter(Integer gunId, Instant from, Instant to) {
    if (gunId != null && gunId < 1) throw new IllegalArgumentException("Model ID must be positive");
    if (from != null && to != null && from.isAfter(to))
      throw new IllegalArgumentException("Start must not be after end");
    return (root, query, cb) -> {
      var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
      if (gunId != null) conditions.add(cb.equal(root.get("gunId"), gunId));
      if (from != null) conditions.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
      if (to != null) conditions.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
      return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
    };
  }
}

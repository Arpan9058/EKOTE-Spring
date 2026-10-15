package com.ekote.controller;

import com.ekote.service.InventoryReportService;
import java.time.Instant;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryReportController {
  private final InventoryReportService reports;

  public InventoryReportController(InventoryReportService reports) {
    this.reports = reports;
  }

  @GetMapping("/summary")
  public InventoryReportService.Summary summary() {
    return reports.summary();
  }

  @GetMapping("/low-stock")
  public InventoryReportService.Result<InventoryReportService.Stock> lowStock(
      @RequestParam(defaultValue = "5") int threshold,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return reports.lowStock(threshold, page, size);
  }

  @GetMapping("/maintenance-due")
  public InventoryReportService.Result<InventoryReportService.Maintenance> due(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return reports.due(page, size);
  }

  @GetMapping("/movements")
  public InventoryReportService.Result<InventoryReportService.Movement> history(
      @RequestParam(required = false) Integer gunId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return reports.history(gunId, from, to, page, size);
  }

  @GetMapping(value = "/movements.csv", produces = "text/csv")
  public ResponseEntity<String> export(
      @RequestParam(required = false) Integer gunId,
      @RequestParam(required = false) Instant from,
      @RequestParam(required = false) Instant to) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock-movements.csv")
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .body(reports.export(gunId, from, to));
  }
}

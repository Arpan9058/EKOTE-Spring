package com.ekote.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GunDetailsDTO {
  private String gunIdentifier;
  private String gunName;
  private String model;
  private int quantity;
  private String registeredBy;
  private LocalDate maintenanceDate;
  private LocalDate nextMaintenanceDate;
  private LocalDateTime issueDate;
  private LocalDateTime returnDate;
  private String status;
  private String userName;
  private String userEmail;
  private String userRole;
}

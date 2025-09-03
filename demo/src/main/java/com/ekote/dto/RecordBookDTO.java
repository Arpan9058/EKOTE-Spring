package com.ekote.dto;

import lombok.Data;
import lombok.AllArgsConstructor; // Add this annotation
import lombok.NoArgsConstructor; // Add this for a default constructor
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor // Added for a default constructor (good practice for DTOs)
@AllArgsConstructor // This will generate the constructor with all fields
public class RecordBookDTO {
    private String uniqueIdentifier;
    private String gunName;
    private String model;
    private String registeredBy;
    private LocalDate manufacturedDate;
    private String manufacturedPlace;
    private LocalDate maintenanceDate;
    private LocalDate nextMaintenanceDate;
    private LocalDateTime issueDate;
    private LocalDateTime returnDate;
    private String status;
    private String name;
    private String email;
    private String role;


    // Remove the manual constructor here.
    // Lombok's @AllArgsConstructor will do this for you.
}
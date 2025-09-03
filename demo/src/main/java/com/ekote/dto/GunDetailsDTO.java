package com.ekote.dto;

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
    private String maintenanceDate;
    private String nextMaintenanceDate;
    private String issueDate;
    private String returnDate;
    private String status;
    private String userName;
    private String userEmail;
    private String userRole;
}

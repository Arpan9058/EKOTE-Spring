package com.ekote.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "gunsDetails")  // adjust to match your DB table
public class GunDetails {

    @Id
    @Column(name = "unique_identifier", nullable = false, unique = true)
    private String uniqueIdentifier; // primary key, like "AK47_M1_X001"

    @Column(name = "gun_name")
    private String gunName;

    @Column(name = "model")
    private String model;

    @Column(name = "quantity")
    private int quantity;

    @Column(name = "registered_by")
    private String registeredBy;

    @Column(name = "manufactured_date")
    private String manufacturedDate;

    @Column(name = "manufactured_place")
    private String manufacturedPlace;

    @Column(name = "maintenance_date")
    private String maintenanceDate;

    @Column(name = "next_maintenance_date")
    private String nextMaintenanceDate;

    @Column(name = "issue_date")
    private String issueDate;

    @Column(name = "return_date")
    private String returnDate;

    @Column(name = "status")
    private String status;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_role")
    private String userRole;

    // --- Getters and Setters ---
    public String getGunIdentifier() {
        return uniqueIdentifier;
    }

    public void setGunIdentifier(String gunIdentifier) {
        this.uniqueIdentifier = gunIdentifier;
    }

    public String getGunName() {
        return gunName;
    }

    public void setGunName(String gunName) {
        this.gunName = gunName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public String getManufacturedDate() {
        return manufacturedDate;
    }

    public void setManufacturedDate(String manufacturedDate) {
        this.manufacturedDate = manufacturedDate;
    }

    public String getManufacturedPlace() {
        return manufacturedPlace;
    }

    public void setManufacturedPlace(String manufacturedPlace) {
        this.manufacturedPlace = manufacturedPlace;
    }

    public String getMaintenanceDate() {
        return maintenanceDate;
    }

    public void setMaintenanceDate(String maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
    }

    public String getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    public void setNextMaintenanceDate(String nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
}

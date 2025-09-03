package com.ekote.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "guns_info")
public class GunInfo {

//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private int id;

    @Id
    @Column(name = "unique_identifier", nullable = false, unique = true)
    private String uniqueIdentifier;

    @ManyToOne
    @JoinColumn(name = "gun_id", nullable = false)
    private Gun gun;

    @Column(name = "manufactured_date", nullable = false)
    private LocalDate manufacturedDate;

    @Column(name = "manufactured_place", nullable = false)
    private String manufacturedPlace;

    // ✅ Maintenance fields
    @Column(name = "maintenance_date")
    private LocalDate maintenanceDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Column(name = "maintenance_freq")
    private Integer maintenanceFreq; // frequency in months

    // Getters & Setters
//    public int getId() { return id; }
//    public void setId(int id) { this.id = id; }

    public String getUniqueIdentifier() { return uniqueIdentifier; }
    public void setUniqueIdentifier(String uniqueIdentifier) { this.uniqueIdentifier = uniqueIdentifier; }

    public Gun getGun() { return gun; }
    public void setGun(Gun gun) { this.gun = gun; }

    public LocalDate getManufacturedDate() { return manufacturedDate; }
    public void setManufacturedDate(LocalDate manufacturedDate) { this.manufacturedDate = manufacturedDate; }

    public String getManufacturedPlace() { return manufacturedPlace; }
    public void setManufacturedPlace(String manufacturedPlace) { this.manufacturedPlace = manufacturedPlace; }

    public LocalDate getMaintenanceDate() { return maintenanceDate; }
    public void setMaintenanceDate(LocalDate maintenanceDate) { this.maintenanceDate = maintenanceDate; }

    public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; }
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) { this.nextMaintenanceDate = nextMaintenanceDate; }

    public Integer getMaintenanceFreq() { return maintenanceFreq; }
    public void setMaintenanceFreq(Integer maintenanceFreq) { this.maintenanceFreq = maintenanceFreq; }
}

package com.ekote.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "guns") // make sure it matches your DB table name
public class Gun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment in MySQL
    private int id;

    @Column(name = "gun_name", nullable = false)
    private String gunName;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "registered_by")
    private String registeredBy;

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}

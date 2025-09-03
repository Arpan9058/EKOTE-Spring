package com.ekote.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "register")
public class User {

    @Id
    private Long id;
    private String name;
    private String password;
    private String mail;
    private String role;

    // Getters and Setters
}

package com.ekote.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "issue")
public class Issue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "gun_id")
    private int gunId;

    @Column(name = "unique_identifier")
    private String uniqueIdentifier;

    private LocalDateTime issueDate;
    private LocalDateTime returnDate;
    private String status;

    // getters and setters
}

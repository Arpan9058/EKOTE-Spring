package com.ekote.dto;

import lombok.Data;

import java.util.List;

@Data
public class GunFormDTO {
    private String gunName;
    private String gunModel;
    private Integer quantity;
    private List<String> identifiers;
    private List<String> manufacturedDates;
    private List<String> manufacturedPlaces;
    // getters and setters
}

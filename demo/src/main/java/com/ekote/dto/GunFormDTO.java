package com.ekote.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.List;

@Data
public class GunFormDTO {
    @NotBlank
    private String gunName;
    @NotBlank
    private String gunModel;
    @NotNull
    @Min(1)
    @Max(1000)
    private Integer quantity;
    @NotEmpty
    private List<@NotBlank String> identifiers;
    @NotEmpty
    private List<@NotBlank String> manufacturedDates;
    @NotEmpty
    private List<@NotBlank String> manufacturedPlaces;
    @AssertTrue(message = "Each item needs an identifier, manufacturing date and place")
    public boolean isCompleteBatch() {
        return quantity != null && identifiers != null && manufacturedDates != null
                && manufacturedPlaces != null && identifiers.size() == quantity
                && manufacturedDates.size() == quantity && manufacturedPlaces.size() == quantity;
    }
}

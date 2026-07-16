package com.ivedi.ivedikuryemanagement.region.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegionRequest(

        @NotBlank(message = "Region name is required")
        @Size(max = 100, message = "Region name must not exceed 100 characters")
        String name,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District must not exceed 100 characters")
        String district,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description
) {}

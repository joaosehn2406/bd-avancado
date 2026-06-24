package com.jps.jps.event.eventByCode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventRequest(
        @NotBlank String state,
        @NotBlank String city,
        @NotNull Integer status,
        Double latitude,
        Double longitude,
        String notes
) {}

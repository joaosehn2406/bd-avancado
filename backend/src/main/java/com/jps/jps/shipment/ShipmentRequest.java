package com.jps.jps.shipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ShipmentRequest(
        @NotBlank String sender,
        @NotBlank String recipient,
        @NotBlank String origin,
        @NotBlank String destination,
        @NotNull @Positive BigDecimal weightKg
) {}

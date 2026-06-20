package com.jps.jps.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String trackingCode,
        String sender,
        String recipient,
        String origin,
        String destination,
        Instant createdAt,
        BigDecimal weightKg
) {}

package com.jps.jps.shipment;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Table("packages")
public record Shipment(

   @PrimaryKey
   String trackingCode,

   String sender,
   String recipient,
   String origin,
   String destination,
   Instant createdAt,
   BigDecimal weightKg

) {}

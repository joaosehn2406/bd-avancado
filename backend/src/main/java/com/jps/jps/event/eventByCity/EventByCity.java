package com.jps.jps.event.eventByCity;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

@Table("events_by_city")
public class EventByCity {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String city;

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private final LocalDate dateBucket;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final String trackingCode;

    private final String status;

    @PersistenceCreator
    public EventByCity(String city, LocalDate dateBucket, Instant timestamp, String trackingCode, String status) {
        this.city = city;
        this.dateBucket = dateBucket;
        this.timestamp = timestamp;
        this.trackingCode = trackingCode;
        this.status = status;
    }

    public String getCity()             { return city; }
    public LocalDate getDateBucket()    { return dateBucket; }
    public Instant getTimestamp()       { return timestamp; }
    public String getTrackingCode()     { return trackingCode; }
    public String getStatus()           { return status; }
}

package com.jps.jps.event.eventByStatus;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;

@Table("events_by_status")
public class EventByStatus {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String status;

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private final LocalDate dateBucket;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 1)
    private final String trackingCode;

    private final String city;

    @PersistenceCreator
    public EventByStatus(String status, LocalDate dateBucket, Instant timestamp, String trackingCode, String city) {
        this.status = status;
        this.dateBucket = dateBucket;
        this.timestamp = timestamp;
        this.trackingCode = trackingCode;
        this.city = city;
    }

    public String getStatus()           { return status; }
    public LocalDate getDateBucket()    { return dateBucket; }
    public Instant getTimestamp()       { return timestamp; }
    public String getTrackingCode()     { return trackingCode; }
    public String getCity()             { return city; }
}

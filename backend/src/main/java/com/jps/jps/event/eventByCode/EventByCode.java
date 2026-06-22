package com.jps.jps.event.eventByCode;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("events_by_code")
public class EventByCode {

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private final String trackingCode;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordinal = 0)
    private final Instant timestamp;

    private final String state;
    private final String city;
    private final String status;
    private final Double latitude;
    private final Double longitude;
    private final String notes;

    @PersistenceCreator
    public EventByCode(String trackingCode, Instant timestamp, String state, String city, String status, Double latitude, Double longitude, String notes) {
        this.trackingCode = trackingCode;
        this.timestamp = timestamp;
        this.state = state;
        this.city = city;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.notes = notes;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }

    public String getStatus() {
        return status;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public String getNotes() {
        return notes;
    }
}

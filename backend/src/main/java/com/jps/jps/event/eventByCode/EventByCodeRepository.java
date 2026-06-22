package com.jps.jps.event.eventByCode;

import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;

public interface EventByCodeRepository extends CassandraRepository<EventByCode, String> {

    List<EventByCode> findByTrackingCode(String trackingCode);
}

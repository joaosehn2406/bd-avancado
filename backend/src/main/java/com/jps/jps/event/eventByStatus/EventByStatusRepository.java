package com.jps.jps.event.eventByStatus;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EventByStatusRepository extends CassandraRepository<EventByStatus, Integer> {

    @Query("SELECT * FROM events_by_status WHERE status = ?0 AND datebucket = ?1")
    List<EventByStatus> findByStatusAndDateBucket(Integer status, LocalDate dateBucket);
}

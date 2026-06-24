package com.jps.jps.event.eventByCity;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface EventByCityRepository extends CassandraRepository<EventByCity, String> {

    @Query("SELECT * FROM events_by_city WHERE city = ?0 AND datebucket = ?1")
    List<EventByCity> findByCityAndDateBucket(String city, LocalDate dateBucket);
}

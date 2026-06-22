package com.jps.jps.shipment;

import org.springframework.data.cassandra.repository.CassandraRepository;

public interface ShipmentRepository extends CassandraRepository<Shipment, String> {}

package com.jps.jps.config;

import com.jps.jps.event.eventByCode.EventStatusConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;

import java.util.List;

@Configuration
public class CassandraConfig {

    @Bean
    public CassandraCustomConversions cassandraCustomConversions() {
        return new CassandraCustomConversions(List.of(
                new EventStatusConverter.Read(),
                new EventStatusConverter.Write()
        ));
    }
}

package com.jps.jps.user;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("users")
public record User(

        @PrimaryKey
        String username,

        String passwordHash,
        Role role,
        Instant createdAt

) {}

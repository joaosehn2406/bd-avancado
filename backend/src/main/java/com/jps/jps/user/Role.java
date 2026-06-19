package com.jps.jps.user;

import java.util.Objects;

public enum Role {

    ADMIN(1);

    private final Integer id;

    Role(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public static Role fromId(Integer id) {
        for (Role role : values()) {
            if(Objects.equals(role.id, id)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unkown role id:" + id);
    }
}

package com.jps.jps.event.eventByCode;

import java.util.Objects;

public enum EventStatus {

    REGISTERED(0, "Registrado"),
    COLLECTED(1, "Coletado"),
    IN_SEPARATION(2, "Em separação"),
    IN_TRANSIT(3, "Em trânsito"),
    OUT_FOR_DELIVERY(4, "Saiu para entrega"),
    DELIVERED(5, "Entregue");

    private final Integer id;
    private final String name;

    EventStatus(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static EventStatus fromId(Integer id) {
        for (EventStatus event : values()) {
            if (Objects.equals(event.id, id)) {
                return event;
            }
        }
        throw new IllegalArgumentException("Unknown status id: " + id);
    }
}

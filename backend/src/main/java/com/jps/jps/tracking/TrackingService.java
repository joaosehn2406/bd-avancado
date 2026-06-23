package com.jps.jps.tracking;

import com.jps.jps.event.eventByCode.EventByCodeService;
import com.jps.jps.event.eventByCode.EventStatus;
import com.jps.jps.event.eventByCode.EventStatusResponse;
import com.jps.jps.event.eventByCode.TimelineEventResponse;
import com.jps.jps.shipment.Shipment;
import com.jps.jps.shipment.ShipmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrackingService {

    private final ShipmentService shipmentService;
    private final EventByCodeService eventByCodeService;

    public TrackingService(ShipmentService shipmentService, EventByCodeService eventByCodeService) {
        this.shipmentService = shipmentService;
        this.eventByCodeService = eventByCodeService;
    }

    public TrackingResponse getByTrackingCode(String trackingCode) {
        Shipment shipment = shipmentService.findByTrackingCode(trackingCode);
        List<TimelineEventResponse> events = eventByCodeService.findByTrackingCode(trackingCode);

        EventStatusResponse currentStatus = events.isEmpty()
                ? EventStatusResponse.from(EventStatus.REGISTERED)
                : events.get(0).status();

        return new TrackingResponse(
                shipment.trackingCode(),
                shipment.origin(),
                shipment.destination(),
                shipment.createdAt(),
                currentStatus,
                events
        );
    }
}

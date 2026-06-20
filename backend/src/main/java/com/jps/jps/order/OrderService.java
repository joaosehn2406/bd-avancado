package com.jps.jps.order;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse findByTrackingCode(String trackingCode) {
        Order order = orderRepository.findById(trackingCode)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return toResponse(order);
    }

    public OrderResponse create(OrderRequest request) {
        Order order = new Order(
                generateTrackingCode(),
                request.sender(),
                request.recipient(),
                request.origin(),
                request.destination(),
                Instant.now(),
                request.weightKg()
        );

        orderRepository.save(order);
        return toResponse(order);
    }

    public void delete(String trackingCode) {
        if (trackingCode == null || trackingCode.isBlank()) {
            throw new IllegalArgumentException("Tracking code cannot be blank");
        }
        orderRepository.deleteById(trackingCode);
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.trackingCode(),
                order.sender(),
                order.recipient(),
                order.origin(),
                order.destination(),
                order.createdAt(),
                order.weightKg()
        );
    }

    private String generateTrackingCode() {
        return "BR" + UUID.randomUUID().toString().replace("-", "").substring(0, 9).toUpperCase();
    }
}

package com.jps.jps.order;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order findByTrackingCode(String trackingCode) {
        return orderRepository.findById(trackingCode)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public Order create(Order order) {
        if (order == null) throw new IllegalArgumentException("Order can not be empty");

        return orderRepository.save(order);
    }

    void delete(String trackingCode) {
        if(trackingCode.isEmpty()) throw new IllegalArgumentException("Trackingcode can not be empty to delete it");

        orderRepository.deleteById(trackingCode);
    }
}

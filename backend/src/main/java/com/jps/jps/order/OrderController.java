package com.jps.jps.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid OrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request));
    }

    @GetMapping("/{trackingCode}")
    public ResponseEntity<OrderResponse> findByTrackingCode(@PathVariable String trackingCode) {
        return ResponseEntity.ok(orderService.findByTrackingCode(trackingCode));
    }

    @DeleteMapping("/{trackingCode}")
    public ResponseEntity<Void> delete(@PathVariable String trackingCode) {
        orderService.delete(trackingCode);
        return ResponseEntity.noContent().build();
    }
}

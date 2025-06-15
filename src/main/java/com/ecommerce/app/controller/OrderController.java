package com.ecommerce.app.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.app.dto.CreateOrderDto;
import com.ecommerce.app.dto.OrderDto;
import com.ecommerce.app.dto.OrderStatusUpdateDto;
import com.ecommerce.app.security.UserPrincipal;
import com.ecommerce.app.service.OrderService;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> createOrder(@RequestBody CreateOrderDto createOrderDto,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto createdOrder = orderService.createOrder(principal, createOrderDto);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @GetMapping("/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDto>> getMyOrders(@Parameter(hidden = true) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        Page<OrderDto> orders = orderService.findOrdersForUser(principal, pageable);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> getMyOrderById(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        OrderDto order = orderService.findUserOrderById(id, principal);
        return ResponseEntity.ok(order);
    }

    // --- ADMIN-ONLY ENDPOINTS ---

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderDto>> getAllOrdersAsAdmin(@Parameter(hidden = true) Pageable pageable) {
        Page<OrderDto> orders = orderService.findAllOrders(pageable);
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderDto> updateOrderStatus(@PathVariable Long id,
            @RequestBody OrderStatusUpdateDto statusUpdateDto) {
        OrderDto updatedOrder = orderService.updateOrderStatus(id, statusUpdateDto);
        return ResponseEntity.ok(updatedOrder);
    }
}

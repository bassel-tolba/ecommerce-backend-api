package com.ecommerce.app.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import com.ecommerce.app.model.Order;
import com.ecommerce.app.model.OrderStatus;

// FIX: Removed @Builder from record.
public record OrderDto(
        Long id,
        String orderNumber,
        Long userId,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal taxes,
        BigDecimal totalAmount,
        AddressDto shippingAddress,
        AddressDto billingAddress,
        String notes,
        Instant createdAt,
        Set<OrderItemDto> items) {
    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getStatus(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getTaxes(),
                order.getTotalAmount(),
                AddressDto.toDto(order.getShippingAddress()),
                AddressDto.toDto(order.getBillingAddress()),
                order.getNotes(),
                order.getCreatedAt(),
                order.getOrderItems().stream().map(OrderItemDto::toDto).collect(Collectors.toSet()));
    }
}

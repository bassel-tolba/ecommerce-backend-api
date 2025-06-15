package com.ecommerce.app.dto;

import java.math.BigDecimal;

import com.ecommerce.app.model.OrderItem;

// FIX: Removed @Builder from record.
public record OrderItemDto(
        Long id,
        String variantSku,
        String productName,
        Integer quantity,
        BigDecimal pricePerUnit) {
    public static OrderItemDto toDto(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemDto(
                item.getId(),
                item.getVariant().getSku(),
                item.getVariant().getProduct().getName(),
                item.getQuantity(),
                item.getPricePerUnit());
    }
}

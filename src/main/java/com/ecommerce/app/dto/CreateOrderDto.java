package com.ecommerce.app.dto;
import java.util.List;
public record CreateOrderDto(Long shippingAddressId, Long billingAddressId, String notes, List<OrderItemRequestDto> items) {}

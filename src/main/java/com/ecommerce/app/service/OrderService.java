package com.ecommerce.app.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.CreateOrderDto;
import com.ecommerce.app.dto.OrderDto;
import com.ecommerce.app.dto.OrderItemRequestDto;
import com.ecommerce.app.dto.OrderStatusUpdateDto;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.Address;
import com.ecommerce.app.model.Order;
import com.ecommerce.app.model.OrderItem;
import com.ecommerce.app.model.OrderStatus;
import com.ecommerce.app.model.ProductVariant;
import com.ecommerce.app.model.User;
import com.ecommerce.app.repository.AddressRepository;
import com.ecommerce.app.repository.OrderRepository;
import com.ecommerce.app.repository.ProductVariantRepository;
import com.ecommerce.app.repository.UserRepository;
import com.ecommerce.app.security.UserPrincipal;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryService inventoryService;

    @Transactional
    public OrderDto createOrder(UserPrincipal currentUser, CreateOrderDto orderData) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Address shippingAddress = addressRepository.findById(orderData.shippingAddressId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Shipping Address", "id", orderData.shippingAddressId()));
        if (!Objects.equals(shippingAddress.getUser().getId(), currentUser.getId())) {
            throw new InvalidOperationException("Shipping address does not belong to the user.");
        }
        // ... Similar check for billing address ...

        inventoryService.checkAndReserveStock(orderData.items());

        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setShippingAddress(shippingAddress);
        newOrder.setBillingAddress(shippingAddress); // Simplified
        newOrder.setStatus(OrderStatus.PENDING);
        newOrder.setOrderNumber(UUID.randomUUID().toString().toUpperCase());
        newOrder.setNotes(orderData.notes());

        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequestDto itemRequest : orderData.items()) {
            ProductVariant variant = productVariantRepository.findBySku(itemRequest.variantSku())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("ProductVariant", "sku", itemRequest.variantSku()));

            OrderItem orderItem = OrderItem.builder().order(newOrder).variant(variant).quantity(itemRequest.quantity())
                    .pricePerUnit(variant.getPrice()).build();
            orderItems.add(orderItem);
            subtotal = subtotal.add(variant.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        newOrder.setOrderItems(orderItems);
        newOrder.setSubtotal(subtotal);
        BigDecimal taxes = subtotal.multiply(new BigDecimal("0.08"));
        BigDecimal shippingCost = new BigDecimal("10.00");
        newOrder.setTaxes(taxes);
        newOrder.setShippingCost(shippingCost);
        newOrder.setTotalAmount(subtotal.add(taxes).add(shippingCost));

        Order savedOrder = orderRepository.save(newOrder);
        return OrderDto.toDto(savedOrder);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> findOrdersForUser(UserPrincipal currentUser, Pageable pageable) {
        return orderRepository.findByUserId(currentUser.getId(), pageable).map(OrderDto::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> findAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderDto::toDto);
    }

    @Transactional(readOnly = true)
    public OrderDto findUserOrderById(Long orderId, UserPrincipal currentUser) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!Objects.equals(order.getUser().getId(), currentUser.getId())
                && !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new InvalidOperationException("You are not authorized to view this order.");
        }
        return OrderDto.toDto(order);
    }

    @Transactional
    public OrderDto updateOrderStatus(Long orderId, OrderStatusUpdateDto statusUpdateDto) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        // Add state machine logic here if needed (e.g., can't cancel a shipped order)
        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOperationException(
                    "Cannot change status of an order that has already been shipped or delivered.");
        }

        if (order.getStatus() != OrderStatus.CANCELLED && statusUpdateDto.status() == OrderStatus.CANCELLED) {
            inventoryService.releaseStock(order);
        }

        order.setStatus(statusUpdateDto.status());
        return OrderDto.toDto(orderRepository.save(order));
    }
}

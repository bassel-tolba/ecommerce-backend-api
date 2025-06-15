package com.ecommerce.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.OrderItemRequestDto;
import com.ecommerce.app.exception.InsufficientStockException;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.InventoryStock;
import com.ecommerce.app.model.Order;
import com.ecommerce.app.model.OrderItem;
import com.ecommerce.app.model.ProductVariant;
import com.ecommerce.app.repository.InventoryStockRepository;
import com.ecommerce.app.repository.ProductVariantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryStockRepository inventoryStockRepository;
    private final ProductVariantRepository productVariantRepository;
    private static final Long DEFAULT_WAREHOUSE_ID = 1L; // Assuming a single warehouse for simplicity

    @Transactional
    public void checkAndReserveStock(List<OrderItemRequestDto> items) {
        for (OrderItemRequestDto item : items) {
            ProductVariant variant = productVariantRepository.findBySku(item.variantSku())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "sku", item.variantSku()));

            // CRITICAL: The repository method MUST use a pessimistic write lock for this to
            // be safe
            InventoryStock stock = inventoryStockRepository
                    .findByVariantIdAndWarehouseId(variant.getId(), DEFAULT_WAREHOUSE_ID)
                    .orElseThrow(() -> new InvalidOperationException(
                            "Stock record not found for SKU: " + item.variantSku()));

            int availableQuantity = stock.getQuantity() - stock.getReservedQuantity();

            if (availableQuantity < item.quantity()) {
                throw new InsufficientStockException("Not enough stock for SKU: " + item.variantSku() +
                        ". Requested: " + item.quantity() + ", Available: " + availableQuantity);
            }

            stock.setReservedQuantity(stock.getReservedQuantity() + item.quantity());
            inventoryStockRepository.save(stock);
        }
    }

    @Transactional
    public void releaseStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            InventoryStock stock = inventoryStockRepository
                    .findByVariantIdAndWarehouseId(item.getVariant().getId(), DEFAULT_WAREHOUSE_ID)
                    .orElseThrow(() -> new InvalidOperationException(
                            "Stock record not found for SKU: " + item.getVariant().getSku()));

            stock.setReservedQuantity(stock.getReservedQuantity() - item.getQuantity());
            inventoryStockRepository.save(stock);
        }
    }
}

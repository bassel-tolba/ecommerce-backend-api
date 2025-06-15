// --- FILE: InventoryService.java ---
package com.ecommerce.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.app.dto.InventoryStockDto;
import com.ecommerce.app.dto.OrderItemRequestDto;
import com.ecommerce.app.dto.StockAdjustmentDto;
import com.ecommerce.app.exception.InsufficientStockException;
import com.ecommerce.app.exception.InvalidOperationException;
import com.ecommerce.app.exception.ResourceNotFoundException;
import com.ecommerce.app.model.InventoryStock;
import com.ecommerce.app.model.Order;
import com.ecommerce.app.model.OrderItem;
import com.ecommerce.app.model.ProductVariant;
import com.ecommerce.app.model.Warehouse;
import com.ecommerce.app.repository.InventoryStockRepository;
import com.ecommerce.app.repository.ProductVariantRepository;
import com.ecommerce.app.repository.WarehouseRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing product inventory stock.
 *
 * This service handles the complete lifecycle of inventory management:
 * 1. Reserving stock when an order is created.
 * 2. Releasing reserved stock if an order is cancelled.
 * 3. Deducting committed stock when an order is shipped.
 * 4. Administrative functions for adding new stock.
 *
 * It uses pessimistic locking to ensure data integrity under concurrent access.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryStockRepository inventoryStockRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository; // Added for stock replenishment

    /**
     * Checks stock availability and reserves the required quantity for a list of
     * items.
     * This operation is atomic and uses pessimistic locking to prevent overselling.
     *
     * @param items       The list of items and quantities being ordered.
     * @param warehouseId The ID of the warehouse from which to reserve stock.
     * @throws InsufficientStockException if available stock is less than requested
     *                                    quantity.
     * @throws ResourceNotFoundException  if a product variant is not found by its
     *                                    SKU.
     * @throws InvalidOperationException  if a stock record for a variant does not
     *                                    exist.
     */
    @Transactional(propagation = Propagation.MANDATORY) // Must be called within an existing transaction (e.g.,
                                                        // createOrder)
    public void checkAndReserveStock(List<OrderItemRequestDto> items, Long warehouseId) {
        for (OrderItemRequestDto item : items) {
            ProductVariant variant = productVariantRepository.findBySku(item.variantSku())
                    .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "sku", item.variantSku()));

            // Use the locking repository method to prevent race conditions
            InventoryStock stock = inventoryStockRepository
                    .findAndLockById(variant.getId(), warehouseId)
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

    /**
     * Deducts committed stock from inventory after an order has been shipped.
     * This reduces both the total quantity and the reserved quantity.
     *
     * @param order       The order that has been shipped.
     * @param warehouseId The ID of the warehouse from which the stock was shipped.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deductCommittedStock(Order order, Long warehouseId) {
        for (OrderItem item : order.getOrderItems()) {
            InventoryStock stock = inventoryStockRepository
                    .findAndLockById(item.getVariant().getId(), warehouseId)
                    .orElseThrow(() -> new InvalidOperationException(
                            "CRITICAL: Stock record disappeared for SKU: " + item.getVariant().getSku()));

            // Ensure the operation is safe
            if (stock.getQuantity() < item.getQuantity() || stock.getReservedQuantity() < item.getQuantity()) {
                // This state should ideally never be reached if reservation logic is correct.
                // It indicates a data inconsistency that must be flagged.
                throw new InvalidOperationException(
                        "CRITICAL: Inconsistent stock state for SKU: " + item.getVariant().getSku());
            }

            stock.setQuantity(stock.getQuantity() - item.getQuantity());
            stock.setReservedQuantity(stock.getReservedQuantity() - item.getQuantity());
            inventoryStockRepository.save(stock);
        }
    }

    /**
     * Releases reserved stock back into the available pool when an order is
     * cancelled.
     *
     * @param order       The order that has been cancelled.
     * @param warehouseId The ID of the warehouse where stock was reserved.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseStock(Order order, Long warehouseId) {
        for (OrderItem item : order.getOrderItems()) {
            InventoryStock stock = inventoryStockRepository
                    .findAndLockById(item.getVariant().getId(), warehouseId)
                    .orElseThrow(() -> new InvalidOperationException(
                            "CRITICAL: Stock record disappeared for SKU: " + item.getVariant().getSku()));

            stock.setReservedQuantity(stock.getReservedQuantity() - item.getQuantity());
            inventoryStockRepository.save(stock);
        }
    }

    /**
     * Adds a specified quantity of stock for a product variant at a warehouse.
     * This is an administrative function for replenishing inventory.
     *
     * @param adjustmentDto DTO containing SKU, warehouse ID, and quantity to add.
     * @return The updated InventoryStock DTO.
     */
    @Transactional
    public InventoryStockDto addStock(StockAdjustmentDto adjustmentDto) {
        ProductVariant variant = productVariantRepository.findBySku(adjustmentDto.variantSku())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "sku", adjustmentDto.variantSku()));

        Warehouse warehouse = warehouseRepository.findById(adjustmentDto.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", "id", adjustmentDto.warehouseId()));

        InventoryStock stock = inventoryStockRepository
                .findAndLockById(variant.getId(), warehouse.getId())
                .orElseGet(() -> {
                    // Create a new stock record if it doesn't exist
                    return InventoryStock.builder()
                            .id(new com.ecommerce.app.model.InventoryStockId(variant.getId(), warehouse.getId()))
                            .variant(variant)
                            .warehouse(warehouse)
                            .quantity(0)
                            .reservedQuantity(0)
                            .build();
                });

        stock.setQuantity(stock.getQuantity() + adjustmentDto.quantity());
        InventoryStock savedStock = inventoryStockRepository.save(stock);

        // You would need an InventoryStockDto for the return type, assuming one exists.
        // For now, returning null or a simplified object.
        // Let's assume an InventoryStockDto.toDto(savedStock) exists.
        return InventoryStockDto.toDto(savedStock);
    }
}
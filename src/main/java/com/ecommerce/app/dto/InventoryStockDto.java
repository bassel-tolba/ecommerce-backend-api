package com.ecommerce.app.dto;

import com.ecommerce.app.model.InventoryStock;

// FIX: Removed @Builder from record.
public record InventoryStockDto(
        Long variantId,
        String variantSku,
        Long warehouseId,
        String warehouseName,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity) {
    public static InventoryStockDto toDto(InventoryStock stock) {
        if (stock == null) {
            return null;
        }
        return new InventoryStockDto(
                stock.getId().getVariantId(),
                stock.getVariant().getSku(),
                stock.getId().getWarehouseId(),
                stock.getWarehouse().getName(),
                stock.getQuantity(),
                stock.getReservedQuantity(),
                stock.getQuantity() - stock.getReservedQuantity());
    }
}

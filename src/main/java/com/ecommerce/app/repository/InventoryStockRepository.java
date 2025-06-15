package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.InventoryStock;
import com.ecommerce.app.model.InventoryStockId;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, InventoryStockId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryStock> findByVariantIdAndWarehouseId(Long variantId, Long warehouseId);

}

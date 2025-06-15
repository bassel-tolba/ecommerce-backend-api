// --- FILE: InventoryStockRepository.java ---
package com.ecommerce.app.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ecommerce.app.model.InventoryStock;
import com.ecommerce.app.model.InventoryStockId;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryStockRepository extends JpaRepository<InventoryStock, InventoryStockId> {

    /**
     * Finds an inventory stock record by variant ID and warehouse ID with a
     * pessimistic write lock.
     * This lock is CRITICAL to prevent race conditions during stock checks and
     * reservations.
     * Any transaction attempting to read this row will be blocked until the current
     * transaction completes.
     *
     * @param variantId   The ID of the product variant.
     * @param warehouseId The ID of the warehouse.
     * @return An Optional containing the locked InventoryStock record if found.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventoryStock s WHERE s.id.variantId = :variantId AND s.id.warehouseId = :warehouseId")
    Optional<InventoryStock> findAndLockById(Long variantId, Long warehouseId);
}
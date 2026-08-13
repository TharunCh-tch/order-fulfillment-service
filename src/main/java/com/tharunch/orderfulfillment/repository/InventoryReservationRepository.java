package com.tharunch.orderfulfillment.repository;

import com.tharunch.orderfulfillment.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    List<InventoryReservation> findByOrderNumber(String orderNumber);

    List<InventoryReservation> findByProductSku(String productSku);
}

package com.ivedi.ivedikuryemanagement.delivery.impl;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryStatusHistoryRepository extends JpaRepository<DeliveryStatusHistory, UUID> {

    List<DeliveryStatusHistory> findAllByDeliveryOrder_IdOrderByChangedAtAsc(UUID deliveryOrderId);
}

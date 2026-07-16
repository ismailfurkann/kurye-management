package com.ivedi.ivedikuryemanagement.delivery.impl;

import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_status_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    private DeliveryOrder deliveryOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 30)
    private DeliveryOrderStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private DeliveryOrderStatus newStatus;

    @Column(name = "changed_by_user_id", nullable = false)
    private UUID changedByUserId;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "note", length = 500)
    private String note;
}

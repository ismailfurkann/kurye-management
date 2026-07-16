package com.ivedi.ivedikuryemanagement.delivery.impl;

import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.delivery.api.OrderPriority;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_orders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @Column(name = "order_code", nullable = false, length = 30, updatable = false)
    private String orderCode;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_address_text", nullable = false, length = 500)
    private String customerAddressText;

    @Column(name = "customer_latitude", precision = 10, scale = 7)
    private BigDecimal customerLatitude;

    @Column(name = "customer_longitude", precision = 10, scale = 7)
    private BigDecimal customerLongitude;

    @Column(name = "order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal orderAmount;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_collection_type", nullable = false, length = 30)
    private PaymentCollectionType paymentCollectionType;

    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DeliveryOrderStatus status = DeliveryOrderStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private OrderPriority priority = OrderPriority.NORMAL;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "picked_up_at")
    private OffsetDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Optimistic lock — prevents two managers assigning to the same order simultaneously. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}

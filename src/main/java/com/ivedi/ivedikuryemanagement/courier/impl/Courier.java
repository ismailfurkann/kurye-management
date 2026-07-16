package com.ivedi.ivedikuryemanagement.courier.impl;

import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.region.impl.Region;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "couriers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Courier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 20)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.OFFLINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "current_latitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(name = "last_location_at")
    private OffsetDateTime lastLocationAt;

    @Column(name = "plate_number", length = 30)
    private String plateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id")
    private Region region;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rating", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "active_assignment_count", nullable = false)
    @Builder.Default
    private int activeAssignmentCount = 0;

    /** Optimistic lock — guards concurrent assignment count updates. */
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

package com.ivedi.ivedikuryemanagement.dispatch.api;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.delivery.api.OrderPriority;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignment;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentResponseMapperTest {

    private final AssignmentResponseMapper mapper = new AssignmentResponseMapper();

    @Test
    void toResponse_mapsFrontendAssignmentFields() {
        User owner = User.builder()
                .id(UUID.randomUUID())
                .fullName("Owner")
                .email("owner@test.com")
                .role(UserRole.RESTAURANT_OWNER)
                .build();
        Restaurant restaurant = Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Ivedi Restoran")
                .owner(owner)
                .build();
        Branch branch = Branch.builder()
                .id(UUID.randomUUID())
                .name("Merkez")
                .restaurant(restaurant)
                .build();
        DeliveryOrder order = DeliveryOrder.builder()
                .id(UUID.randomUUID())
                .branch(branch)
                .orderCode("ORD-1")
                .customerName("Ali")
                .customerPhone("+905551234567")
                .customerAddressText("Adres")
                .orderAmount(new BigDecimal("100.00"))
                .deliveryFee(new BigDecimal("10.00"))
                .paymentCollectionType(PaymentCollectionType.CASH_ON_DELIVERY)
                .deliveryNote("Kapida ara")
                .priority(OrderPriority.NORMAL)
                .status(DeliveryOrderStatus.ASSIGNED)
                .createdBy(owner.getId())
                .build();
        User courierUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Kurye")
                .email("courier@test.com")
                .phone("+905551111111")
                .role(UserRole.COURIER)
                .build();
        Courier courier = Courier.builder()
                .id(UUID.randomUUID())
                .user(courierUser)
                .vehicleType(VehicleType.MOTORBIKE)
                .build();
        DeliveryAssignment assignment = DeliveryAssignment.builder()
                .id(UUID.randomUUID())
                .deliveryOrder(order)
                .courier(courier)
                .status(AssignmentStatus.FAILED)
                .assignedBy(owner.getId())
                .assignedAt(OffsetDateTime.now())
                .failedAt(OffsetDateTime.now())
                .failureReason("Address not found")
                .build();

        AssignmentResponse response = mapper.toResponse(assignment);

        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.orderCode()).isEqualTo("ORD-1");
        assertThat(response.courierId()).isEqualTo(courier.getId());
        assertThat(response.courierName()).isEqualTo("Kurye");
        assertThat(response.courierPhone()).isEqualTo("+905551111111");
        assertThat(response.courierVehicleType()).isEqualTo(VehicleType.MOTORBIKE);
        assertThat(response.restaurantName()).isEqualTo("Ivedi Restoran");
        assertThat(response.branchName()).isEqualTo("Merkez");
        assertThat(response.customerAddress()).isEqualTo("Adres");
        assertThat(response.orderAmount()).isEqualByComparingTo("100.00");
        assertThat(response.deliveryFee()).isEqualByComparingTo("10.00");
        assertThat(response.paymentCollectionType()).isEqualTo(PaymentCollectionType.CASH_ON_DELIVERY);
        assertThat(response.notes()).isEqualTo("Kapida ara");
        assertThat(response.customerName()).isEqualTo("Ali");
        assertThat(response.customerPhone()).isEqualTo("+905551234567");
        assertThat(response.failureReason()).isEqualTo("Address not found");
    }
}

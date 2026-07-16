package com.ivedi.ivedikuryemanagement.dispatch.api;

import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignment;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import org.springframework.stereotype.Component;

@Component
public class AssignmentResponseMapper {

    public AssignmentResponse toResponse(DeliveryAssignment assignment) {
        if (assignment == null) {
            return null;
        }

        DeliveryOrder order = assignment.getDeliveryOrder();
        Branch branch = order != null ? order.getBranch() : null;
        Restaurant restaurant = branch != null ? branch.getRestaurant() : null;
        Courier courier = assignment.getCourier();

        return new AssignmentResponse(
                assignment.getId(),
                order != null ? order.getId() : null,
                order != null ? order.getOrderCode() : null,
                courier != null ? courier.getId() : null,
                courier != null && courier.getUser() != null ? courier.getUser().getFullName() : null,
                courier != null && courier.getUser() != null ? courier.getUser().getPhone() : null,
                courier != null ? courier.getVehicleType() : null,
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getAcceptedAt(),
                assignment.getPickedUpAt(),
                assignment.getDeliveredAt(),
                assignment.getFailedAt(),
                assignment.getCancelledAt(),
                assignment.getFailureReason(),
                restaurant != null ? restaurant.getName() : null,
                branch != null ? branch.getName() : null,
                order != null ? order.getCustomerAddressText() : null,
                order != null ? order.getOrderAmount() : null,
                order != null ? order.getDeliveryFee() : null,
                order != null ? order.getPaymentCollectionType() : null,
                order != null ? order.getDeliveryNote() : null,
                order != null ? order.getCustomerName() : null,
                order != null ? order.getCustomerPhone() : null
        );
    }
}

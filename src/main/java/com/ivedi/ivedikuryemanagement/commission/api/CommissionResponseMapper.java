package com.ivedi.ivedikuryemanagement.commission.api;

import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.commission.impl.Commission;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CommissionResponseMapper {

    public CommissionResponse toResponse(Commission commission) {
        Courier courier = commission.getCourier();
        User user = courier != null ? courier.getUser() : null;
        DeliveryOrder order = commission.getDeliveryOrder();
        return new CommissionResponse(
                commission.getId(),
                courier != null ? courier.getId() : null,
                user != null ? user.getFullName() : null,
                order != null ? order.getId() : null,
                order != null ? order.getOrderCode() : null,
                valueOrZero(commission.getGrossAmount()),
                valueOrZero(commission.getCommissionRate()),
                valueOrZero(commission.getNetAmount()),
                commission.getStatus(),
                commission.getPaymentPeriod(),
                commission.getPaidAt(),
                commission.getCreatedAt()
        );
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

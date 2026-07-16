package com.ivedi.ivedikuryemanagement.delivery.web;

import com.ivedi.ivedikuryemanagement.delivery.api.OrderPriority;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateDeliveryOrderRequest(

        @Size(max = 100)
        String customerName,

        @Size(max = 20)
        String customerPhone,

        @Size(max = 500)
        String customerAddressText,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal customerLatitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        BigDecimal customerLongitude,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal orderAmount,

        @DecimalMin(value = "0.0")
        BigDecimal deliveryFee,

        PaymentCollectionType paymentCollectionType,

        @Size(max = 500)
        String deliveryNote,

        OrderPriority priority
) {}

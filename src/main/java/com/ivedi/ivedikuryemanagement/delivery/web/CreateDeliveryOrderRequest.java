package com.ivedi.ivedikuryemanagement.delivery.web;

import com.ivedi.ivedikuryemanagement.delivery.api.OrderPriority;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDeliveryOrderRequest(

        @NotNull(message = "Branch ID is required")
        UUID branchId,

        @NotBlank(message = "Order code is required")
        @Size(max = 30)
        String orderCode,

        @NotBlank(message = "Customer name is required")
        @Size(max = 100)
        String customerName,

        @NotBlank(message = "Customer phone is required")
        @Size(max = 20)
        String customerPhone,

        @NotBlank(message = "Customer address is required")
        @Size(max = 500)
        String customerAddressText,

        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0")
        BigDecimal customerLatitude,

        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0")
        BigDecimal customerLongitude,

        @NotNull(message = "Order amount is required")
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal orderAmount,

        @NotNull(message = "Delivery fee is required")
        @DecimalMin(value = "0.0")
        BigDecimal deliveryFee,

        @NotNull(message = "Payment collection type is required")
        PaymentCollectionType paymentCollectionType,

        @Size(max = 500)
        String deliveryNote,

        OrderPriority priority
) {}

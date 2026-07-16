package com.ivedi.ivedikuryemanagement.delivery.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.delivery.api.*;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DeliveryOrderControllerTest {

    private final DeliveryOrderService deliveryOrderService = mock(DeliveryOrderService.class);
    private final DeliveryOrderController controller = new DeliveryOrderController(deliveryOrderService);

    @Test
    void ordersAlias_returnsPageResponseAndPassesFilters() {
        OrderResponse item = orderResponse("READY_FOR_DISPATCH");
        when(deliveryOrderService.searchResponses(any())).thenReturn(new PageImpl<>(List.of(item)));

        ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> response = controller.getOrders(
                "READY_FOR_DISPATCH",
                item.branchId(),
                item.restaurantId(),
                "ORD",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31),
                OrderPriority.HIGH,
                0,
                20
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).containsExactly(item);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);

        ArgumentCaptor<DeliveryOrderSearchDto> captor = ArgumentCaptor.forClass(DeliveryOrderSearchDto.class);
        verify(deliveryOrderService).searchResponses(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(DeliveryOrderStatus.READY_FOR_DISPATCH);
        assertThat(captor.getValue().responseStatus()).isEqualTo("READY_FOR_DISPATCH");
        assertThat(captor.getValue().search()).isEqualTo("ORD");
        assertThat(captor.getValue().priority()).isEqualTo(OrderPriority.HIGH);
    }

    @Test
    void ordersAlias_acceptStatusDoesNotRequireOrderEnumValue() {
        OrderResponse item = orderResponse("ACCEPTED");
        when(deliveryOrderService.searchResponses(any())).thenReturn(new PageImpl<>(List.of(item)));

        controller.getOrders("ACCEPTED", null, null, null, null, null, null, 0, 20);

        ArgumentCaptor<DeliveryOrderSearchDto> captor = ArgumentCaptor.forClass(DeliveryOrderSearchDto.class);
        verify(deliveryOrderService).searchResponses(captor.capture());
        assertThat(captor.getValue().status()).isNull();
        assertThat(captor.getValue().responseStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void writeAndHistoryAliasesDelegateToFrontendResponseMethods() {
        UUID id = UUID.randomUUID();
        OrderResponse order = orderResponse("CREATED");
        CreateDeliveryOrderRequest createRequest = createRequest(order.branchId());
        UpdateDeliveryOrderRequest updateRequest = new UpdateDeliveryOrderRequest(
                "Ayse", null, null, null, null, null, null, null, null, null);
        CancelDeliveryOrderRequest cancelRequest = new CancelDeliveryOrderRequest("Customer cancelled");
        StatusHistoryResponse history = new StatusHistoryResponse(
                UUID.randomUUID(), id, DeliveryOrderStatus.CREATED,
                DeliveryOrderStatus.READY_FOR_DISPATCH, UUID.randomUUID(),
                "Owner", "Submitted", OffsetDateTime.now());

        when(deliveryOrderService.createResponse(createRequest)).thenReturn(order);
        when(deliveryOrderService.updateResponse(id, updateRequest)).thenReturn(order);
        when(deliveryOrderService.getResponseById(id)).thenReturn(order);
        when(deliveryOrderService.submitForDispatchResponse(id)).thenReturn(order);
        when(deliveryOrderService.cancelResponse(id, cancelRequest)).thenReturn(order);
        when(deliveryOrderService.getStatusHistoryResponses(id)).thenReturn(List.of(history));
        when(deliveryOrderService.getReadyResponsesForDispatch()).thenReturn(List.of(order));

        assertThat(controller.createAlias(createRequest).getBody().getData()).isEqualTo(order);
        assertThat(controller.updateAlias(id, updateRequest).getBody().getData()).isEqualTo(order);
        assertThat(controller.getByIdAlias(id).getBody().getData()).isEqualTo(order);
        assertThat(controller.submitAlias(id).getBody().getData()).isEqualTo(order);
        assertThat(controller.cancelAlias(id, cancelRequest).getBody().getData()).isEqualTo(order);
        assertThat(controller.getStatusHistoryAlias(id).getBody().getData()).containsExactly(history);
        assertThat(controller.getReadyForDispatchAlias().getBody().getData()).containsExactly(order);
    }

    @Test
    void legacyEndpointsStillDelegateToLegacyMethods() {
        UUID id = UUID.randomUUID();
        DeliveryOrderDto dto = new DeliveryOrderDto(
                id,
                "ORD-1",
                UUID.randomUUID(),
                "Merkez",
                "Restoran",
                "Ali",
                "+905551234567",
                "Adres",
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                PaymentCollectionType.CASH_ON_DELIVERY,
                null,
                DeliveryOrderStatus.CREATED,
                OrderPriority.NORMAL,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(deliveryOrderService.getById(id)).thenReturn(dto);

        assertThat(controller.getById(id).getBody().getData()).isEqualTo(dto);
        verify(deliveryOrderService).getById(id);
    }

    private CreateDeliveryOrderRequest createRequest(UUID branchId) {
        return new CreateDeliveryOrderRequest(
                branchId,
                "ORD-1",
                "Ali",
                "+905551234567",
                "Adres",
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                PaymentCollectionType.CASH_ON_DELIVERY,
                "Not",
                OrderPriority.NORMAL
        );
    }

    private OrderResponse orderResponse(String status) {
        UUID orderId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        AssignmentResponse assignment = "ACCEPTED".equals(status)
                ? new AssignmentResponse(
                UUID.randomUUID(), orderId, "ORD-1", UUID.randomUUID(),
                "Kurye", "+905551111111", null, AssignmentStatus.ACCEPTED,
                OffsetDateTime.now(), OffsetDateTime.now(), null, null, null,
                null, null, "Restoran", "Merkez", "Adres",
                new BigDecimal("100.00"), new BigDecimal("10.00"),
                PaymentCollectionType.CASH_ON_DELIVERY, "Not", "Ali", "+905551234567")
                : null;
        return new OrderResponse(
                orderId,
                "ORD-1",
                branchId,
                "Merkez",
                restaurantId,
                "Restoran",
                "Ali",
                "+905551234567",
                "Adres",
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("110.00"),
                PaymentCollectionType.CASH_ON_DELIVERY,
                OrderPriority.NORMAL,
                "Not",
                status,
                List.of(),
                assignment,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}

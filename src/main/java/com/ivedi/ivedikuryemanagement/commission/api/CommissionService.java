package com.ivedi.ivedikuryemanagement.commission.api;

import org.springframework.data.domain.Page;

import java.util.UUID;

public interface CommissionService {

    /**
     * Creates a commission record for a completed delivery.
     * Called internally when assignment status reaches DELIVERED.
     * Uses REQUIRES_NEW so commission failure never rolls back the delivery.
     */
    CommissionDto createForDelivery(UUID assignmentId);

    /**
     * Returns paginated commission history for the currently authenticated courier.
     */
    Page<CommissionDto> getMyCommissions(int page, int size);

    Page<CommissionResponse> getMyCommissionResponses(int page, int size);

    /**
     * Returns the earnings summary for the current courier.
     * Optionally filtered by payment period (YYYY-MM).
     */
    EarningsSummaryDto getMyEarningsSummary(String period);

    CommissionSummaryResponse getMyFrontendSummary();

    /**
     * Admin: marks PENDING commissions as PAID for a given courier and period.
     */
    int markAsPaid(UUID courierId, String period);

    CommissionResponse markOneAsPaid(UUID id);

    /**
     * Admin: paginated commission search across all couriers.
     */
    Page<CommissionDto> search(UUID courierId, CommissionStatus status, String period, int page, int size);

    Page<CommissionResponse> searchResponses(UUID courierId, CommissionStatus status, String period, int page, int size);

    CommissionResponse getResponseById(UUID id);
}

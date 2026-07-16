package com.ivedi.ivedikuryemanagement.courier.api;

import com.ivedi.ivedikuryemanagement.courier.web.CourierApprovalRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierAvailabilityRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierLocationRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierRegistrationRequest;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface CourierService {

    // ── Courier self ──────────────────────────────────────────────────────────

    /**
     * Creates a courier profile for the currently authenticated COURIER user.
     * Requires the user to have the COURIER role. Profile starts in PENDING approval.
     * Throws DuplicateResourceException if a profile already exists.
     */
    CourierDto createProfile(CourierRegistrationRequest request);

    /**
     * Returns the courier profile of the currently authenticated courier.
     */
    CourierDto getMyProfile();

    /**
     * Changes the availability of the current courier.
     * Only APPROVED couriers can change availability.
     * BUSY status is system-managed and cannot be set directly.
     */
    CourierDto updateAvailability(CourierAvailabilityRequest request);

    /**
     * Updates the real-time GPS location of the current courier.
     */
    CourierDto updateLocation(CourierLocationRequest request);

    CourierResponse getMyProfileResponse();

    CourierResponse updateAvailabilityResponse(CourierAvailabilityRequest request);

    CourierResponse updateLocationResponse(CourierLocationRequest request);

    // ── Admin / Manager ───────────────────────────────────────────────────────

    /**
     * Returns a courier profile by internal courier id.
     */
    CourierDto getById(UUID id);

    CourierResponse getResponseById(UUID id);

    /**
     * Paginated, filtered list of couriers.
     */
    Page<CourierDto> search(CourierSearchDto searchDto);

    Page<CourierResponse> searchResponses(CourierSearchDto searchDto);

    /**
     * Returns all APPROVED + AVAILABLE couriers, sorted by rating descending.
     * Used by the manager dispatch panel.
     */
    List<CourierDto> getAvailableForDispatch();

    List<CourierResponse> getAvailableResponsesForDispatch();

    /**
     * Sets the approval status of a courier.
     * BLOCKED and REJECTED automatically force availability to OFFLINE.
     */
    CourierDto setApprovalStatus(UUID id, CourierApprovalRequest request);

    CourierResponse setApprovalStatusResponse(UUID id, CourierApprovalRequest request);

    Page<DeliveryAssignmentDto> getAssignments(UUID courierId, AssignmentStatus status, int page, int size);

    // ── Internal — called by the assignment / dispatch module ─────────────────

    /**
     * Increments active assignment count and sets availability to BUSY.
     * Called when a delivery is assigned to this courier.
     */
    void incrementActiveCount(UUID courierId);

    /**
     * Decrements active assignment count.
     * When count reaches 0 and courier is APPROVED, sets availability back to AVAILABLE.
     */
    void decrementActiveCount(UUID courierId);
}

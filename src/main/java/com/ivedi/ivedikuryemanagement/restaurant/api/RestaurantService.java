package com.ivedi.ivedikuryemanagement.restaurant.api;

import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantOwnerRequest;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantRequest;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantStatusRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface RestaurantService {

    /**
     * Creates a restaurant and assigns it to the specified owner.
     * The owner must have the RESTAURANT_OWNER role.
     * New restaurants start in PENDING status.
     */
    RestaurantDto create(RestaurantRequest request);

    /**
     * Updates name, contact, and tax info of an existing restaurant.
     * Does not change status or owner — use dedicated endpoints for those.
     */
    RestaurantDto update(UUID id, RestaurantRequest request);

    /**
     * Returns a restaurant by id. Admin and manager access only.
     */
    RestaurantDto getById(UUID id);

    /**
     * Paginated filtered list of all restaurants. Admin and manager access only.
     */
    Page<RestaurantDto> search(RestaurantSearchDto searchDto);

    /**
     * Frontend-facing paginated restaurant list with derived metrics.
     */
    Page<RestaurantResponse> searchResponses(RestaurantSearchDto searchDto);

    /**
     * Returns all restaurants owned by the currently authenticated user.
     * Enforces data isolation — owner can only see their own restaurants.
     */
    List<RestaurantDto> getMyRestaurants();

    /**
     * Frontend-facing restaurants owned by the current user.
     */
    List<RestaurantResponse> getMyRestaurantResponses();

    /**
     * Returns a specific restaurant owned by the currently authenticated user.
     * Returns NOT_FOUND if the restaurant exists but belongs to another owner.
     */
    RestaurantDto getMyRestaurantById(UUID id);

    /**
     * Frontend-facing restaurant read with role-aware owner isolation.
     */
    RestaurantResponse getVisibleResponseById(UUID id);

    RestaurantResponse createResponse(RestaurantRequest request);

    RestaurantResponse updateResponse(UUID id, RestaurantRequest request);

    RestaurantResponse setStatusResponse(UUID id, RestaurantStatusRequest request);

    RestaurantResponse changeOwnerResponse(UUID id, RestaurantOwnerRequest request);

    /**
     * Changes the operational status of a restaurant.
     * Only PLATFORM_ADMIN can activate or suspend a restaurant.
     */
    RestaurantDto setStatus(UUID id, RestaurantStatusRequest request);

    /**
     * Transfers restaurant ownership to another RESTAURANT_OWNER user.
     */
    RestaurantDto changeOwner(UUID id, RestaurantOwnerRequest request);
}

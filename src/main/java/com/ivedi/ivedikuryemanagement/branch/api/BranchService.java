package com.ivedi.ivedikuryemanagement.branch.api;

import com.ivedi.ivedikuryemanagement.branch.web.BranchRequest;
import com.ivedi.ivedikuryemanagement.branch.web.BranchStatusRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface BranchService {

    /**
     * Creates a new branch for a restaurant in the specified region.
     * Region must be ACTIVE. Branch name must be unique within the restaurant.
     */
    BranchDto create(BranchRequest request);

    /**
     * Updates branch contact, address, and region.
     * Restaurant cannot be changed after creation — create a new branch instead.
     */
    BranchDto update(UUID id, BranchRequest request);

    /**
     * Returns a branch by id. Admin and manager access only.
     */
    BranchDto getById(UUID id);

    /**
     * Paginated filtered list. Filterable by restaurantId, regionId, name, status.
     * Admin and manager access only.
     */
    Page<BranchDto> search(BranchSearchDto searchDto);

    /**
     * Frontend-facing paginated branch list.
     */
    Page<BranchResponse> searchResponses(BranchSearchDto searchDto);

    /**
     * Returns all ACTIVE branches for a specific restaurant.
     * For owner access: verifies the restaurant belongs to the current user.
     * For admin/manager: no ownership check.
     */
    List<BranchDto> getByRestaurantId(UUID restaurantId);

    List<BranchResponse> getByRestaurantIdResponses(UUID restaurantId);

    List<BranchResponse> getByRestaurantIdResponsesForCurrentUser(UUID restaurantId);

    /**
     * Returns all branches of a restaurant owned by the current user.
     * Returns NOT_FOUND if the restaurant does not belong to the current user.
     */
    List<BranchDto> getMyBranches(UUID restaurantId);

    List<BranchResponse> getMyBranchResponses();

    /**
     * Returns a single branch of a restaurant owned by the current user.
     * Returns NOT_FOUND if the restaurant or branch does not belong to the current user.
     */
    BranchDto getMyBranchById(UUID restaurantId, UUID branchId);

    BranchResponse getResponseById(UUID id);

    BranchResponse createResponse(BranchRequest request);

    BranchResponse updateResponse(UUID id, BranchRequest request);

    BranchResponse toggleStatus(UUID id);

    /**
     * Activates or deactivates a branch.
     */
    BranchDto setStatus(UUID id, BranchStatusRequest request);
}

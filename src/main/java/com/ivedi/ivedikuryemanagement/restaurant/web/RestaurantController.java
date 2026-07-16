package com.ivedi.ivedikuryemanagement.restaurant.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantPermissions;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantResponse;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantSearchDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantService;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    // ── Admin write endpoints ─────────────────────────────────────────────────

    @PostMapping("/api/v1/admin/restaurants")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantDto>> create(
            @Valid @RequestBody RestaurantRequest request
    ) {
        RestaurantDto created = restaurantService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created", created));
    }

    @PatchMapping("/api/v1/admin/restaurants/{id}")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantDto>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated", restaurantService.update(id, request)));
    }

    @PatchMapping("/api/v1/admin/restaurants/{id}/status")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantDto>> setStatus(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant status updated", restaurantService.setStatus(id, request)));
    }

    @PatchMapping("/api/v1/admin/restaurants/{id}/owner")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantDto>> changeOwner(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantOwnerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant owner updated", restaurantService.changeOwner(id, request)));
    }

    @PostMapping("/api/v1/restaurants")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantResponse>> createAlias(
            @Valid @RequestBody RestaurantRequest request
    ) {
        RestaurantResponse created = restaurantService.createResponse(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created", created));
    }

    @PutMapping("/api/v1/restaurants/{id}")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateAlias(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated", restaurantService.updateResponse(id, request)));
    }

    @PatchMapping("/api/v1/restaurants/{id}/status")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantResponse>> setStatusAlias(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant status updated", restaurantService.setStatusResponse(id, request)));
    }

    @PostMapping("/api/v1/restaurants/{id}/assign-owner")
    @PreAuthorize(RestaurantPermissions.ADMIN_ONLY)
    public ResponseEntity<ApiResponse<RestaurantResponse>> assignOwnerAlias(
            @PathVariable UUID id,
            @Valid @RequestBody RestaurantOwnerRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant owner updated", restaurantService.changeOwnerResponse(id, request)));
    }

    // ── Admin / Manager read endpoints ────────────────────────────────────────

    @GetMapping("/api/v1/admin/restaurants")
    @PreAuthorize(RestaurantPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<RestaurantResponse>>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) RestaurantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<RestaurantResponse> result = restaurantService.searchResponses(new RestaurantSearchDto(name, status, page, size));
        return ResponseEntity.ok(ApiResponse.success("Restaurants retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/restaurants")
    @PreAuthorize(RestaurantPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<PageResponse<RestaurantResponse>>> searchAlias(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RestaurantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        Page<RestaurantResponse> result = restaurantService.searchResponses(new RestaurantSearchDto(search, status, page, size));
        return ResponseEntity.ok(ApiResponse.success("Restaurants retrieved", PageResponse.from(result)));
    }

    @GetMapping("/api/v1/admin/restaurants/{id}")
    @PreAuthorize(RestaurantPermissions.ADMIN_OR_MANAGER)
    public ResponseEntity<ApiResponse<RestaurantDto>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant retrieved", restaurantService.getById(id)));
    }

    @GetMapping("/api/v1/restaurants/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PLATFORM_MANAGER', 'RESTAURANT_OWNER')")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getByIdAlias(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant retrieved", restaurantService.getVisibleResponseById(id)));
    }

    // ── Owner endpoints — data isolation enforced at service layer ─────────────

    @GetMapping("/api/v1/restaurants/mine")
    @PreAuthorize(RestaurantPermissions.OWNER_ONLY)
    public ResponseEntity<ApiResponse<List<RestaurantResponse>>> getMyRestaurants() {
        return ResponseEntity.ok(ApiResponse.success("Your restaurants retrieved", restaurantService.getMyRestaurantResponses()));
    }

    @GetMapping("/api/v1/restaurants/mine/{id}")
    @PreAuthorize(RestaurantPermissions.OWNER_ONLY)
    public ResponseEntity<ApiResponse<RestaurantDto>> getMyRestaurantById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant retrieved", restaurantService.getMyRestaurantById(id)));
    }
}

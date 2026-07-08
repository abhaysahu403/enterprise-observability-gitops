package com.enterprise.asset.controller;

import com.enterprise.asset.dto.AssetAssignRequest;
import com.enterprise.asset.dto.AssetRequest;
import com.enterprise.asset.dto.AssetResponse;
import com.enterprise.asset.service.AssetService;
import com.enterprise.shared.dto.ApiResponse;
import com.enterprise.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Asset Management", description = "Track company assets: assignment, return, status, warranty")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Register a new asset")
    public ResponseEntity<ApiResponse<AssetResponse>> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Asset created", assetService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an asset by id")
    public ResponseEntity<ApiResponse<AssetResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assetService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List assets with optional status/type filters and pagination")
    public ResponseEntity<ApiResponse<PageResponse<AssetResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(assetService.list(status, type, pageable)));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "List assets currently assigned to an employee")
    public ResponseEntity<ApiResponse<List<AssetResponse>>> byEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(ApiResponse.success(assetService.byEmployee(employeeId)));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Assign an available asset to an employee")
    public ResponseEntity<ApiResponse<AssetResponse>> assign(@PathVariable Long id,
                                                               @Valid @RequestBody AssetAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Asset assigned", assetService.assign(id, request)));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Return an assigned asset")
    public ResponseEntity<ApiResponse<AssetResponse>> returnAsset(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Asset returned", assetService.returnAsset(id)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @Operation(summary = "Update asset status (e.g. UNDER_REPAIR, RETIRED)")
    public ResponseEntity<ApiResponse<AssetResponse>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Asset status updated", assetService.updateStatus(id, status)));
    }

    @GetMapping("/reports/count-by-status")
    @Operation(summary = "Count of assets in a given status")
    public ResponseEntity<ApiResponse<Long>> countByStatus(@RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(assetService.countByStatus(status)));
    }
}

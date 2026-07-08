package com.enterprise.asset.dto;

import com.enterprise.asset.entity.Asset;

import java.time.Instant;
import java.time.LocalDate;

public class AssetResponse {

    private Long id;
    private String assetTag;
    private String type;
    private String model;
    private String vendor;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpiry;
    private String status;
    private Long assignedToEmployeeId;
    private String assignedToName;
    private Instant assignedAt;
    private Instant returnedAt;

    public static AssetResponse from(Asset a) {
        AssetResponse dto = new AssetResponse();
        dto.id = a.getId();
        dto.assetTag = a.getAssetTag();
        dto.type = a.getType().name();
        dto.model = a.getModel();
        dto.vendor = a.getVendor();
        dto.purchaseDate = a.getPurchaseDate();
        dto.warrantyExpiry = a.getWarrantyExpiry();
        dto.status = a.getStatus().name();
        dto.assignedToEmployeeId = a.getAssignedToEmployeeId();
        dto.assignedToName = a.getAssignedToName();
        dto.assignedAt = a.getAssignedAt();
        dto.returnedAt = a.getReturnedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public String getType() { return type; }
    public String getModel() { return model; }
    public String getVendor() { return vendor; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public LocalDate getWarrantyExpiry() { return warrantyExpiry; }
    public String getStatus() { return status; }
    public Long getAssignedToEmployeeId() { return assignedToEmployeeId; }
    public String getAssignedToName() { return assignedToName; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getReturnedAt() { return returnedAt; }
}

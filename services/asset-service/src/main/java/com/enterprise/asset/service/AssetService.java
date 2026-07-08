package com.enterprise.asset.service;

import com.enterprise.asset.dto.AssetAssignRequest;
import com.enterprise.asset.dto.AssetRequest;
import com.enterprise.asset.dto.AssetResponse;
import com.enterprise.asset.entity.Asset;
import com.enterprise.asset.entity.AssetStatus;
import com.enterprise.asset.entity.AssetType;
import com.enterprise.asset.repository.AssetRepository;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        Asset asset = new Asset();
        asset.setType(parseType(request.getType()));
        asset.setModel(request.getModel());
        asset.setVendor(request.getVendor());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setWarrantyExpiry(request.getWarrantyExpiry());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssetTag(generateAssetTag(asset.getType()));

        Asset saved = assetRepository.save(asset);
        log.info("Asset created: id={} tag={} type={}", saved.getId(), saved.getAssetTag(), saved.getType());
        return AssetResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AssetResponse getById(Long id) {
        return AssetResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> list(String status, String type, Pageable pageable) {
        Page<Asset> page;
        if (status != null) {
            page = assetRepository.findByStatus(parseStatus(status), pageable);
        } else if (type != null) {
            page = assetRepository.findByType(parseType(type), pageable);
        } else {
            page = assetRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(AssetResponse::from));
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> byEmployee(Long employeeId) {
        return assetRepository.findByAssignedToEmployeeId(employeeId).stream()
                .map(AssetResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AssetResponse assign(Long assetId, AssetAssignRequest request) {
        Asset asset = findOrThrow(assetId);

        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new BusinessException("ASSET_NOT_AVAILABLE",
                    "Asset " + asset.getAssetTag() + " is not available (current status: " + asset.getStatus() + ")");
        }

        asset.setStatus(AssetStatus.ASSIGNED);
        asset.setAssignedToEmployeeId(request.getEmployeeId());
        asset.setAssignedToName(request.getEmployeeName());
        asset.setAssignedAt(Instant.now());
        asset.setReturnedAt(null);

        Asset saved = assetRepository.save(asset);
        log.info("Asset assigned: id={} tag={} to employeeId={}", saved.getId(), saved.getAssetTag(), request.getEmployeeId());
        return AssetResponse.from(saved);
    }

    @Transactional
    public AssetResponse returnAsset(Long assetId) {
        Asset asset = findOrThrow(assetId);

        if (asset.getStatus() != AssetStatus.ASSIGNED) {
            throw new BusinessException("ASSET_NOT_ASSIGNED", "Asset " + asset.getAssetTag() + " is not currently assigned");
        }

        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setReturnedAt(Instant.now());
        Long previousHolder = asset.getAssignedToEmployeeId();
        asset.setAssignedToEmployeeId(null);
        asset.setAssignedToName(null);

        Asset saved = assetRepository.save(asset);
        log.info("Asset returned: id={} tag={} previousHolder={}", saved.getId(), saved.getAssetTag(), previousHolder);
        return AssetResponse.from(saved);
    }

    @Transactional
    public AssetResponse updateStatus(Long assetId, String status) {
        Asset asset = findOrThrow(assetId);
        AssetStatus newStatus = parseStatus(status);

        if (newStatus == AssetStatus.ASSIGNED) {
            throw new BusinessException("USE_ASSIGN_ENDPOINT", "Use the /assign endpoint to assign an asset");
        }

        asset.setStatus(newStatus);
        if (newStatus == AssetStatus.AVAILABLE || newStatus == AssetStatus.RETIRED) {
            asset.setAssignedToEmployeeId(null);
            asset.setAssignedToName(null);
        }

        Asset saved = assetRepository.save(asset);
        log.info("Asset status updated: id={} newStatus={}", saved.getId(), newStatus);
        return AssetResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return assetRepository.countByStatus(parseStatus(status));
    }

    private Asset findOrThrow(Long id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
    }

    private AssetType parseType(String value) {
        try {
            return AssetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_ASSET_TYPE", "Unknown asset type: " + value);
        }
    }

    private AssetStatus parseStatus(String value) {
        try {
            return AssetStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_ASSET_STATUS", "Unknown asset status: " + value);
        }
    }

    private String generateAssetTag(AssetType type) {
        long next = assetRepository.count() + 1;
        return String.format("%s-%05d", type.name().substring(0, 3), next);
    }
}

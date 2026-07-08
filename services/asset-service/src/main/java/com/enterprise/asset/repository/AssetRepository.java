package com.enterprise.asset.repository;

import com.enterprise.asset.entity.Asset;
import com.enterprise.asset.entity.AssetStatus;
import com.enterprise.asset.entity.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findByAssetTag(String assetTag);

    boolean existsByAssetTag(String assetTag);

    Page<Asset> findByStatus(AssetStatus status, Pageable pageable);

    Page<Asset> findByType(AssetType type, Pageable pageable);

    List<Asset> findByAssignedToEmployeeId(Long employeeId);

    long countByStatus(AssetStatus status);
}

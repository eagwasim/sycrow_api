package com.sycrow.api.repository;

import com.sycrow.api.model.PlatformAttributeEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;

import java.util.Optional;

public interface PlatformAttributeEntityRepository extends DatastoreRepository<PlatformAttributeEntity, Long> {
    Optional<PlatformAttributeEntity> findFirstByName(String name);
}

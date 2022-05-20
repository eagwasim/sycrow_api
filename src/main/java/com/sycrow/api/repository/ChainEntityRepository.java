package com.sycrow.api.repository;

import com.sycrow.api.model.ChainEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;

import java.util.Optional;

public interface ChainEntityRepository extends DatastoreRepository<ChainEntity, Long> {
    Optional<ChainEntity> findFirstByChainId(String chainId);
}

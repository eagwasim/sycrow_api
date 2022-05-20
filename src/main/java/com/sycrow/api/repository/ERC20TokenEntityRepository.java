package com.sycrow.api.repository;

import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.model.ChainEntity;
import com.sycrow.api.model.ERC20TokenEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;

import java.util.List;
import java.util.Optional;

public interface ERC20TokenEntityRepository extends DatastoreRepository<ERC20TokenEntity, Long> {
    List<ERC20TokenEntity> findAllByChainAndStatus(ChainEntity chain, EntityStatusConstant status);

    Optional<ERC20TokenEntity> findFirstByChainAndContract(ChainEntity chain, String contract);
}

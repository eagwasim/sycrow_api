package com.sycrow.api.repository;

import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.model.ChainEntity;
import com.sycrow.api.model.ERC20TokenEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface ERC20TokenEntityRepository extends DatastoreRepository<ERC20TokenEntity, Long> {
    Slice<ERC20TokenEntity> findAllByChainAndStatus(ChainEntity chain, EntityStatusConstant status, Pageable page);

    Slice<ERC20TokenEntity> findAllByChainAndStatusAndSymbolGreaterThanEqualAndSymbolLessThan(ChainEntity chain, EntityStatusConstant status, String minSearch, String maxSearch, Pageable page);

    Optional<ERC20TokenEntity> findFirstByChainAndContract(ChainEntity chain, String contract);
}

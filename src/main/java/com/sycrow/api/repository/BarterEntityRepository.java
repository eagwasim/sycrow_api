package com.sycrow.api.repository;

import com.sycrow.api.model.BarterEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface BarterEntityRepository extends DatastoreRepository<BarterEntity, Long> {
    Optional<BarterEntity> findFirstByChainIdAndTransactionId(String chainId, String transactionId);

    Optional<BarterEntity> findFirstByChainIdAndBarterContract(String chainId, String contractAddress);

    Slice<BarterEntity> findAllByChainId(String chainId, Pageable pageRequest);

    Slice<BarterEntity> findAllByChainIdAndDepositTokenContract(String chainId, String depositContract, Pageable pageRequest);

    Slice<BarterEntity> findAllByChainIdAndExpectedTokenContract(String chainId, String expectedTokenContract, Pageable pageRequest);

    Slice<BarterEntity> findAllByChainIdAndDepositTokenContractAndExpectedTokenContract(String chainId, String depositContract, String expectedTokenContract, Pageable pageRequest);
}

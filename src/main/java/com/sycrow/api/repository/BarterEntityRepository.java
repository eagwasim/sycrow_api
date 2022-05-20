package com.sycrow.api.repository;

import com.sycrow.api.model.BarterEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BarterEntityRepository extends DatastoreRepository<BarterEntity, Long> {
    Optional<BarterEntity> findFirstByChainIdAndTransactionId(String chainId, String transactionId);

    Optional<BarterEntity> findFirstByChainIdAndBarterContract(String chainId, String contractAddress);

    Page<BarterEntity> findAllByChainId(String chainId, Pageable pageRequest);

    Page<BarterEntity> findAllByChainIdAndDepositTokenContract(String chainId, String depositContract, Pageable pageRequest);

    Page<BarterEntity> findAllByChainIdAndExpectedTokenContract(String chainId, String expectedTokenContract, Pageable pageRequest);

    Page<BarterEntity> findAllByChainIdAndDepositTokenContractAndExpectedTokenContract(String chainId, String depositContract, String expectedTokenContract, Pageable pageRequest);
}

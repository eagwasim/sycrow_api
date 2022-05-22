package com.sycrow.api.repository;

import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.model.BarterEntity;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.Optional;

public interface BarterEntityRepository extends DatastoreRepository<BarterEntity, Long> {
    Optional<BarterEntity> findFirstByChainIdAndTransactionId(String chainId, String transactionId);

    Optional<BarterEntity> findFirstByChainIdAndBarterContract(String chainId, String contractAddress);

    Slice<BarterEntity> findAllByChainIdAndStatus(String chainId, EntityStatusConstant status, Pageable pageRequest);

    Slice<BarterEntity> findAllByChainIdAndDepositTokenContractAndStatus(String chainId, String depositContract, EntityStatusConstant status, Pageable pageRequest);

    Slice<BarterEntity> findAllByChainIdAndExpectedTokenContractAndStatus(String chainId, String expectedTokenContract, EntityStatusConstant status, Pageable pageRequest);

    Slice<BarterEntity> findAllByChainIdAndDepositTokenContractAndExpectedTokenContractAndStatus(String chainId, String depositContract, String expectedTokenContract, EntityStatusConstant status, Pageable pageRequest);
}

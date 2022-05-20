package com.sycrow.api.service.impl;

import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.dto.ChainModel;
import com.sycrow.api.dto.CreateChainModel;
import com.sycrow.api.exception.DuplicateEntityException;
import com.sycrow.api.model.ChainEntity;
import com.sycrow.api.repository.ChainEntityRepository;
import com.sycrow.api.service.ChainService;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.Optional;

@Named
public class ChainServiceImpl implements ChainService {
    private final ChainEntityRepository chainEntityRepository;

    public ChainServiceImpl(ChainEntityRepository chainEntityRepository) {
        this.chainEntityRepository = chainEntityRepository;
    }

    @Override
    public ChainModel createChain(CreateChainModel model) {
        Optional<ChainEntity> optionalChainEntity = chainEntityRepository.findFirstByChainId(model.getChainId());

        if (optionalChainEntity.isPresent()) {
            throw new DuplicateEntityException();
        }

        ChainEntity chain = ChainEntity.builder()
                .chainId(model.getChainId())
                .explorerUrl(model.getExplorerUrl())
                .name(model.getName())
                .tokenSymbol(model.getTokenSymbol())
                .build();

        chain.setDateCreated(LocalDateTime.now());
        chain.setDateModified(LocalDateTime.now());
        chain.setStatus(EntityStatusConstant.ACTIVE);

        chainEntityRepository.save(chain);

        return ChainModel.builder()
                .chainId(model.getChainId())
                .explorerUrl(model.getExplorerUrl())
                .name(model.getName())
                .status(chain.getStatus())
                .tokenSymbol(chain.getTokenSymbol())
                .build();
    }
}

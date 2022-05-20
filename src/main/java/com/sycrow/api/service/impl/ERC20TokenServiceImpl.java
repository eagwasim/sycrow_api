package com.sycrow.api.service.impl;

import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.dto.CreateERC20TokenModel;
import com.sycrow.api.dto.ERC20TokenModel;
import com.sycrow.api.exception.ChainNotSupportedException;
import com.sycrow.api.exception.DuplicateEntityException;
import com.sycrow.api.exception.ERC20TokenNotFoundException;
import com.sycrow.api.model.ChainEntity;
import com.sycrow.api.model.ERC20TokenEntity;
import com.sycrow.api.repository.ChainEntityRepository;
import com.sycrow.api.repository.ERC20TokenEntityRepository;
import com.sycrow.api.service.ERC20TokenService;

import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Named
public class ERC20TokenServiceImpl implements ERC20TokenService {
    private final ERC20TokenEntityRepository erc20TokenEntityRepository;
    private final ChainEntityRepository chainEntityRepository;

    public ERC20TokenServiceImpl(ERC20TokenEntityRepository erc20TokenEntityRepository, ChainEntityRepository chainEntityRepository) {
        this.erc20TokenEntityRepository = erc20TokenEntityRepository;
        this.chainEntityRepository = chainEntityRepository;
    }

    @Override
    public List<ERC20TokenModel> getActiveTokens(String chainID) {
        ChainEntity chain = chainEntityRepository.findFirstByChainId(chainID).orElseThrow(ChainNotSupportedException::new);
        List<ERC20TokenEntity> chainTokens = erc20TokenEntityRepository.findAllByChainAndStatus(chain, EntityStatusConstant.ACTIVE);
        return chainTokens.stream().map(this::from)
                .collect(Collectors.toList());
    }

    @Override
    public ERC20TokenModel createToken(CreateERC20TokenModel model) {
        ChainEntity chain = chainEntityRepository.findFirstByChainId(model.getChainId()).orElseThrow(ChainNotSupportedException::new);
        Optional<ERC20TokenEntity> optionalERC20TokenEntity = erc20TokenEntityRepository.findFirstByChainAndContract(chain, model.getContract());

        if (optionalERC20TokenEntity.isPresent()) {
            throw new DuplicateEntityException();
        }

        ERC20TokenEntity erc20TokenEntity = ERC20TokenEntity.builder()
                .chain(chain)
                .contract(model.getContract())
                .iconUri(model.getIconUri())
                .isNative(model.getIsNative())
                .isWrapped(model.getIsWrapped())
                .name(model.getName())
                .symbol(model.getSymbol())
                .build();

        erc20TokenEntity.setDateCreated(LocalDateTime.now());
        erc20TokenEntity.setDateModified(LocalDateTime.now());
        erc20TokenEntity.setStatus(EntityStatusConstant.INACTIVE);

        erc20TokenEntityRepository.save(erc20TokenEntity);

        return from(erc20TokenEntity);
    }

    @Override
    public void activateToken(Long tokenID) {
        ERC20TokenEntity erc20TokenEntity = erc20TokenEntityRepository.findById(tokenID).orElseThrow(ERC20TokenNotFoundException::new);
        if (erc20TokenEntity.getStatus() != EntityStatusConstant.ACTIVE) {
            erc20TokenEntity.setStatus(EntityStatusConstant.ACTIVE);
            erc20TokenEntityRepository.save(erc20TokenEntity);
        }
    }

    private ERC20TokenModel from(ERC20TokenEntity c) {
        return ERC20TokenModel.builder()
                .chainId(c.getChain().getChainId())
                .contract(c.getContract())
                .id(c.getId())
                .iconUri(c.getIconUri())
                .isNative(c.getIsNative())
                .isWrapped(c.getIsWrapped())
                .status(c.getStatus())
                .name(c.getName())
                .symbol(c.getSymbol())
                .build();
    }
}

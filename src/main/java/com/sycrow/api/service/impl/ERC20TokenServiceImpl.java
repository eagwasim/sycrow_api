package com.sycrow.api.service.impl;

import com.google.common.base.Strings;
import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.dto.CreateERC20TokenModel;
import com.sycrow.api.dto.ERC20TokenModel;
import com.sycrow.api.dto.ERC20TokenSearchResponseModel;
import com.sycrow.api.exception.ChainNotSupportedException;
import com.sycrow.api.exception.ERC20TokenNotFoundException;
import com.sycrow.api.model.ChainEntity;
import com.sycrow.api.model.ERC20TokenEntity;
import com.sycrow.api.repository.ChainEntityRepository;
import com.sycrow.api.repository.ERC20TokenEntityRepository;
import com.sycrow.api.service.ERC20TokenService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import javax.inject.Named;
import java.time.LocalDateTime;
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
    public ERC20TokenSearchResponseModel getActiveTokens(String chainID, String name, int page, int limit) {
        ChainEntity chain = chainEntityRepository.findFirstByChainId(chainID).orElseThrow(ChainNotSupportedException::new);

        Slice<ERC20TokenEntity> chainTokens;

        if (Strings.isNullOrEmpty(name)) {
            PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(
                    Sort.Order.desc("isNative"),
                    Sort.Order.desc("priority"),
                    Sort.Order.asc("symbol")));
            chainTokens = erc20TokenEntityRepository.findAllByChainAndStatus(chain, EntityStatusConstant.ACTIVE, pageRequest);
        } else {
            String min = name.toUpperCase();
            String max = String.valueOf('\ufffd');
            PageRequest pageRequest = PageRequest.of(page, limit, Sort.by(
                    Sort.Order.asc("symbol"),
                    Sort.Order.desc("isNative"),
                    Sort.Order.desc("priority")
            ));
            chainTokens = erc20TokenEntityRepository.findAllByChainAndStatusAndSymbolGreaterThanEqualAndSymbolLessThan(chain, EntityStatusConstant.ACTIVE, min, min + max, pageRequest);
        }

        return ERC20TokenSearchResponseModel.builder()
                .tokens(chainTokens.stream().map(this::from)
                        .collect(Collectors.toList()))
                .hasMore(chainTokens.hasNext())
                .build();
    }

    @Override
    public ERC20TokenModel getByChainAndContract(String chainId, String contractId) {
        ChainEntity chain = chainEntityRepository.findFirstByChainId(chainId).orElseThrow(ChainNotSupportedException::new);
        Optional<ERC20TokenEntity> optionalERC20TokenEntity = erc20TokenEntityRepository.findFirstByChainAndContract(chain, contractId.toLowerCase());

        if (optionalERC20TokenEntity.isEmpty()) {
            return null;
        }

        return from(optionalERC20TokenEntity.get());
    }

    @Override
    public ERC20TokenModel createToken(CreateERC20TokenModel model) {
        ChainEntity chain = chainEntityRepository.findFirstByChainId(model.getChainId()).orElseThrow(ChainNotSupportedException::new);
        Optional<ERC20TokenEntity> optionalERC20TokenEntity = erc20TokenEntityRepository.findFirstByChainAndContract(chain, model.getContract().toLowerCase());

        if (optionalERC20TokenEntity.isPresent()) {
            ERC20TokenEntity oldToken = optionalERC20TokenEntity.get();
            oldToken.setStatus(EntityStatusConstant.ACTIVE);
            erc20TokenEntityRepository.save(oldToken);
            return from(oldToken);
        }

        ERC20TokenEntity erc20TokenEntity = ERC20TokenEntity.builder()
                .chain(chain)
                .contract(model.getContract().toLowerCase())
                .iconUri(model.getIconUri())
                .isNative(model.getIsNative())
                .isWrapped(model.getIsWrapped())
                .name(model.getName())
                .symbol(model.getSymbol().toUpperCase())
                .priority(model.getPriority())
                .cmcId(model.getCmcId())
                .slug(model.getSlug())
                .build();

        erc20TokenEntity.setDateCreated(LocalDateTime.now());
        erc20TokenEntity.setDateModified(LocalDateTime.now());
        erc20TokenEntity.setStatus(EntityStatusConstant.ACTIVE);

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
                .contract(c.getContract().toLowerCase())
                .id(c.getId())
                .iconUri(c.getIconUri())
                .isNative(c.getIsNative())
                .isWrapped(c.getIsWrapped())
                .status(c.getStatus())
                .name(c.getName())
                .symbol(c.getSymbol())
                .cmcId(c.getCmcId())
                .priority(c.getPriority())
                .slug(c.getSlug())
                .build();
    }
}

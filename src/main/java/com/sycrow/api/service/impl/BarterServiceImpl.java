package com.sycrow.api.service.impl;

import com.google.common.base.Strings;
import com.sycrow.api.blockchain.Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory;
import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.constant.PlatformAttributeNamesConstant;
import com.sycrow.api.dto.BarterFilterModel;
import com.sycrow.api.dto.BarterModel;
import com.sycrow.api.dto.BarterSearchResponseModel;
import com.sycrow.api.exception.SecretValueNotFoundException;
import com.sycrow.api.model.BarterEntity;
import com.sycrow.api.repository.BarterEntityRepository;
import com.sycrow.api.service.BarterService;
import com.sycrow.api.service.helpers.PlatformAttributeHelper;
import io.reactivex.Flowable;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.web3j.abi.EventEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;

import javax.inject.Named;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

@Named
@Log4j2
public class BarterServiceImpl implements BarterService {
    private static final String BARTER_CREATION_HASH = EventEncoder.encode(Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SYCROWBARTERCREATED_EVENT);
    private static final String BARTER_TRADE_HASH = EventEncoder.encode(Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SYCROWTRADEBYBARTER_EVENT);
    private static final String BARTER_WITHDRAW_HASH = EventEncoder.encode(Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SYCROWWITHDRAWFROMBARTER_EVENT);

    private static final String ADMIN_PRIVATE_KEY_NAME_ENV_KEY = "network.account.keys.private.name";
    private static final String ADMIN_PRIVATE_KEY_VERSION_ENV_KEY = "network.account.keys.private.version";
    private static final String BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY = "factories.barter.token.";
    private static final String NETWORK_URL_ENV_KEY = "networks.url.";

    private final BarterEntityRepository barterEntityRepository;
    private final PlatformAttributeHelper platformAttributeHelper;

    private final Environment environment;

    public BarterServiceImpl(BarterEntityRepository barterEntityRepository, PlatformAttributeHelper platformAttributeHelper, Environment environment) {
        this.barterEntityRepository = barterEntityRepository;
        this.platformAttributeHelper = platformAttributeHelper;
        this.environment = environment;
    }

    private void processBarterCreationEvent(String chainId, Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowBarterCreatedEventResponse eventResponse) {
        Optional<BarterEntity> optionalBarterEntity = barterEntityRepository.findFirstByChainIdAndTransactionId(chainId, eventResponse.log.getTransactionHash());

        if (optionalBarterEntity.isPresent()) {
            return;
        }

        BarterEntity barterEntity = BarterEntity.builder()
                .transactionId(eventResponse.log.getTransactionHash())
                .barterContract(eventResponse._barter)
                .account(eventResponse._createdBy)
                .chainId(chainId)
                .deadline(fromUTCTimeStampMins(eventResponse._deadline.longValue()))
                .depositTokenContract(eventResponse._inToken)
                .expectedTokenContract(eventResponse._outToken)
                .build();

        barterEntity.setDateCreated(LocalDateTime.now());
        barterEntity.setDateModified(LocalDateTime.now());
        barterEntity.setStatus(EntityStatusConstant.ACTIVE);

        barterEntityRepository.save(barterEntity);
    }

    private void processBarterTradeEvent(String chainId, Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowTradeByBarterEventResponse eventResponse) {
        //Optional<BarterEntity> optionalBarterEntity = barterEntityRepository.findFirstByChainIdAndBarterContract(chainId, eventResponse._barter);
        //TODO
    }

    private void processBarterWithdrawalEvent(String chainId, Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowWithdrawFromBarterEventResponse eventResponse) {
        Optional<BarterEntity> optionalBarterEntity = barterEntityRepository.findFirstByChainIdAndBarterContract(chainId, eventResponse._barter);
        if (optionalBarterEntity.isEmpty()) {
            return;
        }

        BarterEntity barterEntity = optionalBarterEntity.get();

        barterEntity.setStatus(EntityStatusConstant.COMPLETED);
        barterEntity.setDateModified(LocalDateTime.now());

        barterEntityRepository.save(barterEntity);
    }

    @Override
    public void processBarterCreationEvents(String chainId) {
        Optional<String> lbs = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_CREATION_L_B_S_.getNameForChain(chainId));

        AtomicReference<String> latestBlockScanned = lbs.map(AtomicReference::new).orElseGet(() -> new AtomicReference<>(null));
        DefaultBlockParameter defaultBlockParameter = Optional.ofNullable(latestBlockScanned.get()).map(s -> DefaultBlockParameter.valueOf(new BigInteger(s, 16))).orElse(DefaultBlockParameterName.EARLIEST);

        String contractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId);

        EthFilter eventFilter = new EthFilter(defaultBlockParameter, DefaultBlockParameterName.LATEST, contractAddress);
        eventFilter.addSingleTopic(BARTER_CREATION_HASH);

        try {
            Flowable<Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowBarterCreatedEventResponse> transferEventResponseFlowable = factory.syCrowBarterCreatedEventFlowable(eventFilter);
            StreamSupport.stream(transferEventResponseFlowable.blockingIterable().spliterator(), false)
                    .forEach(((Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowBarterCreatedEventResponse eventResponse) -> {
                        String block = eventResponse.log.getBlockNumber().toString(16);
                        latestBlockScanned.set(block);

                        if (eventResponse._isPrivate == Boolean.FALSE) {
                            this.processBarterCreationEvent(chainId, eventResponse);
                        }
                    }));
        } catch (Throwable e) {
            log.error(e);
        }
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_CREATION_L_B_S_.getNameForChain(chainId), latestBlockScanned.get());
    }

    @Override
    public void processBarterTradeEvents(String chainId) {
        Optional<String> lbs = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_TRADE_L_B_S.getNameForChain(chainId));

        AtomicReference<String> latestBlockScanned = lbs.map(AtomicReference::new).orElseGet(() -> new AtomicReference<>(null));
        DefaultBlockParameter defaultBlockParameter = Optional.ofNullable(latestBlockScanned.get()).map(s -> DefaultBlockParameter.valueOf(new BigInteger(s, 16))).orElse(DefaultBlockParameterName.EARLIEST);

        String contractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId);

        EthFilter eventFilter = new EthFilter(defaultBlockParameter, DefaultBlockParameterName.LATEST, contractAddress);
        eventFilter.addSingleTopic(BARTER_TRADE_HASH);

        try {
            Flowable<Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowTradeByBarterEventResponse> eventFlowable = factory.syCrowTradeByBarterEventFlowable(eventFilter);
            StreamSupport.stream(eventFlowable.blockingIterable().spliterator(), false)
                    .forEach(((Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowTradeByBarterEventResponse eventResponse) -> {
                        String block = eventResponse.log.getBlockNumber().toString(16);
                        latestBlockScanned.set(block);
                        this.processBarterTradeEvent(chainId, eventResponse);
                    }));
        } catch (Throwable e) {
            log.error(e);
        }
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_TRADE_L_B_S.getNameForChain(chainId), latestBlockScanned.get());
    }

    @Override
    public void processBarterWithdrawalEvents(String chainId) {
        Optional<String> lbs = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_WITHDRAWAL_L_B_S.getNameForChain(chainId));

        AtomicReference<String> latestBlockScanned = lbs.map(AtomicReference::new).orElseGet(() -> new AtomicReference<>(null));
        DefaultBlockParameter defaultBlockParameter = Optional.ofNullable(latestBlockScanned.get()).map(s -> DefaultBlockParameter.valueOf(new BigInteger(s, 16))).orElse(DefaultBlockParameterName.EARLIEST);

        String contractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId);

        EthFilter eventFilter = new EthFilter(defaultBlockParameter, DefaultBlockParameterName.LATEST, contractAddress);
        eventFilter.addSingleTopic(BARTER_WITHDRAW_HASH);

        try {
            Flowable<Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowWithdrawFromBarterEventResponse> eventFlowable = factory.syCrowWithdrawFromBarterEventFlowable(eventFilter);
            StreamSupport.stream(eventFlowable.blockingIterable().spliterator(), false)
                    .forEach(((Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.SyCrowWithdrawFromBarterEventResponse eventResponse) -> {
                        String block = eventResponse.log.getBlockNumber().toString(16);
                        latestBlockScanned.set(block);
                        this.processBarterWithdrawalEvent(chainId, eventResponse);
                    }));
        } catch (Throwable e) {
            log.error(e);
        }
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_WITHDRAWAL_L_B_S.getNameForChain(chainId), latestBlockScanned.get());
    }

    @Override
    public BarterSearchResponseModel getBarters(String chainID, BarterFilterModel filterModel) {
        PageRequest pageRequest = PageRequest.of(filterModel.getStart(), filterModel.getLimit(), Sort.by(Sort.Order.desc("dateCreated")));

        Page<BarterEntity> barterEntityPage;

        if (!Strings.isNullOrEmpty(filterModel.getDepositedTokenAddress()) && !Strings.isNullOrEmpty(filterModel.getExpectsTokenAddress())) {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndDepositTokenContractAndExpectedTokenContract(chainID, filterModel.getDepositedTokenAddress(), filterModel.getExpectsTokenAddress(), pageRequest);
        } else if (!Strings.isNullOrEmpty(filterModel.getDepositedTokenAddress())) {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndDepositTokenContract(chainID, filterModel.getDepositedTokenAddress(), pageRequest);
        } else if (!Strings.isNullOrEmpty(filterModel.getExpectsTokenAddress())) {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndExpectedTokenContract(chainID, filterModel.getExpectsTokenAddress(), pageRequest);
        } else {
            barterEntityPage = barterEntityRepository.findAllByChainId(chainID, pageRequest);
        }

        return BarterSearchResponseModel.builder()
                .totalCount(barterEntityPage.getTotalElements())
                .barters(barterEntityPage.map(b -> BarterModel.builder().contractAddress(b.getBarterContract()).build()).toList())
                .build();
    }

    private LocalDateTime fromUTCTimeStampMins(Long timeStamp) {
        return Instant.ofEpochMilli(timeStamp * 1000).atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    private Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory forChain(String chainId) {
        String factoryContractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));

        Web3j web3 = Web3j.build(new HttpService(environment.getProperty(String.format("%s%s", NETWORK_URL_ENV_KEY, chainId))));

        String secretKeyName = environment.getProperty(ADMIN_PRIVATE_KEY_NAME_ENV_KEY);
        String secretKeyVersion = environment.getProperty(ADMIN_PRIVATE_KEY_VERSION_ENV_KEY);

        Credentials credentials = Credentials.create(platformAttributeHelper.getSecretValue(secretKeyName, secretKeyVersion).orElseThrow(() -> new SecretValueNotFoundException(secretKeyName + ":" + secretKeyVersion)));

        TransactionManager transactionManager = new RawTransactionManager(web3, credentials, Integer.parseInt(chainId));

        return Contracts_SycrowBarterFactory_sol_SyCrowBarterFactory.load(factoryContractAddress, web3, transactionManager, new DefaultGasProvider());
    }
}

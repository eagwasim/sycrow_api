package com.sycrow.api.service.impl;

import com.google.common.base.Strings;
import com.sycrow.api.blockchain.SyCrowBarterFactory_sol_SyCrowBarterFactory;
import com.sycrow.api.blockchain.SyCrowBarter_sol_SyCrowBarter;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.web3j.abi.EventEncoder;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;
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
    private static final String BARTER_CREATION_HASH = EventEncoder.encode(SyCrowBarterFactory_sol_SyCrowBarterFactory.CREATION_EVENT);
    private static final String BARTER_TRADE_HASH = EventEncoder.encode(SyCrowBarterFactory_sol_SyCrowBarterFactory.TRADE_EVENT);
    private static final String BARTER_WITHDRAW_HASH = EventEncoder.encode(SyCrowBarterFactory_sol_SyCrowBarterFactory.COMPLETION_EVENT);

    private static final String ADMIN_PRIVATE_KEY_NAME_ENV_KEY = "network.account.keys.private.name";
    private static final String ADMIN_PRIVATE_KEY_VERSION_ENV_KEY = "network.account.keys.private.version";
    private static final String BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY = "barter.token.factory.address.";
    private static final String NETWORK_URL_ENV_KEY = "networks.url.";

    private final BarterEntityRepository barterEntityRepository;
    private final PlatformAttributeHelper platformAttributeHelper;

    private final Environment environment;

    public BarterServiceImpl(BarterEntityRepository barterEntityRepository, PlatformAttributeHelper platformAttributeHelper, Environment environment) {
        this.barterEntityRepository = barterEntityRepository;
        this.platformAttributeHelper = platformAttributeHelper;
        this.environment = environment;
    }

    void processBarterCreationEvent(String chainId, SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse eventResponse) {
        Optional<BarterEntity> optionalBarterEntity = barterEntityRepository.findFirstByChainIdAndTransactionId(chainId, eventResponse.log.getTransactionHash().toLowerCase());

        if (optionalBarterEntity.isPresent()) {
            return;
        }

        BarterEntity barterEntity = BarterEntity.builder()
                .transactionId(eventResponse.log.getTransactionHash().toLowerCase())
                .barterContract(eventResponse.barter.toLowerCase())
                .account(eventResponse.createdBy.toLowerCase())
                .chainId(chainId)
                .deadline(fromUTCTimeStampMins(eventResponse.deadline.longValue()))
                .depositTokenContract(eventResponse.inToken.toLowerCase())
                .expectedTokenContract(eventResponse.outToken.toLowerCase())
                .build();

        barterEntity.setDateCreated(LocalDateTime.now());
        barterEntity.setDateModified(LocalDateTime.now());
        barterEntity.setStatus(EntityStatusConstant.ACTIVE);

        barterEntityRepository.save(barterEntity);
    }

    void processBarterTradeEvent(String chainId, SyCrowBarterFactory_sol_SyCrowBarterFactory.TradeEventResponse eventResponse) {
        log.info("New Barter Trade! ----- " + eventResponse.barter + " ---- " + eventResponse.inAmount.toString() + " ---- " + eventResponse.outAmount.toString());
    }

    void processBarterWithdrawalEvent(String chainId, SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse eventResponse) {
        log.info("New Barter Withdraw! ----- " + eventResponse.barter + " ---- ");
        Optional<BarterEntity> optionalBarterEntity = barterEntityRepository.findFirstByChainIdAndBarterContract(chainId, eventResponse.barter);
        if (optionalBarterEntity.isEmpty()) {
            log.info("BarterWithdraw Not Found: Barter with contract id => " + eventResponse.barter + " not found for withdraw");
            return;
        }

        BarterEntity barterEntity = optionalBarterEntity.get();

        barterEntity.setStatus(EntityStatusConstant.COMPLETED);
        barterEntity.setDateModified(LocalDateTime.now());

        barterEntityRepository.save(barterEntity);
        log.info("BarterWithdraw Complete: Barter with contract id => " + eventResponse.barter + " Completed! ");
    }

    @Override
    public void processNewBarters(String chainId) {
        Optional<String> lip = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_CREATION_L_I_P_.getNameForChain(chainId));
        BigInteger lastIndexProcessed = lip.map(BigInteger::new).orElseGet(() -> new BigInteger("0"));
        String accountSecret = getPrivateKey();
        SyCrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId, accountSecret);
        try {
            BigInteger totalBarterSize = factory.allBartersLength().send();

            if (lastIndexProcessed.compareTo(totalBarterSize) >= 0) {
                return;
            }
            for (BigInteger index = lastIndexProcessed; index.compareTo(totalBarterSize) < 0; index = index.add(new BigInteger("1"))) {
                String barterAddress = factory.allBarters(index).send();
                processBarter(chainId, barterAddress, accountSecret);
                lastIndexProcessed = index;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_CREATION_L_I_P_.getNameForChain(chainId), lastIndexProcessed.toString());

    }

    void processBarter(String chainId, String barterAddress, String secret) throws Exception {
        Optional<BarterEntity> optionalBarterEntity = barterEntityRepository.findFirstByChainIdAndBarterContract(chainId, barterAddress);

        if (optionalBarterEntity.isPresent()) {
            return;
        }

        SyCrowBarter_sol_SyCrowBarter syCrowBarter_sol_syCrowBarter = fromContract(chainId, barterAddress, secret);
        String owner = syCrowBarter_sol_syCrowBarter.owner().send();
        BigInteger deadline = syCrowBarter_sol_syCrowBarter.deadline().send();
        Tuple2<String, String> tokens = syCrowBarter_sol_syCrowBarter.getTokens().send();

        BarterEntity barterEntity = BarterEntity.builder()
                .transactionId(null)
                .barterContract(barterAddress)
                .account(owner.toLowerCase())
                .chainId(chainId)
                .deadline(fromUTCTimeStampMins(deadline.longValue()))
                .depositTokenContract(tokens.component1().toLowerCase())
                .expectedTokenContract(tokens.component2().toLowerCase())
                .build();

        barterEntity.setDateCreated(LocalDateTime.now());
        barterEntity.setDateModified(LocalDateTime.now());
        barterEntity.setStatus(EntityStatusConstant.ACTIVE);

        barterEntityRepository.save(barterEntity);
    }

    @Override
    @Deprecated
    public void processBarterCreationEvents(String chainId) {
        Optional<String> lbs = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_CREATION_L_B_S_.getNameForChain(chainId));

        AtomicReference<String> latestBlockScanned = lbs.map(AtomicReference::new).orElseGet(() -> new AtomicReference<>(null));

        DefaultBlockParameter defaultBlockParameter = Optional.ofNullable(latestBlockScanned.get()).map(s -> DefaultBlockParameter.valueOf(new BigInteger(s, 16))).orElse(DefaultBlockParameterName.EARLIEST);

        String contractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        SyCrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId, getPrivateKey());

        EthFilter eventFilter = new EthFilter(defaultBlockParameter, DefaultBlockParameterName.LATEST, contractAddress);
        eventFilter.addSingleTopic(BARTER_CREATION_HASH);

        try {
            Flowable<SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse> transferEventResponseFlowable = factory.creationEventFlowable(eventFilter);
            StreamSupport.stream(transferEventResponseFlowable.blockingIterable().spliterator(), false)
                    .forEach(((SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse eventResponse) -> {
                        String block = eventResponse.log.getBlockNumber().toString(16);
                        // Storing the second-latest block scanned instead of the first to stop the filter not found error from breaking cron
                        latestBlockScanned.set(block);

                        if (eventResponse.isPrivate == Boolean.FALSE) {
                            this.processBarterCreationEvent(chainId, eventResponse);
                        }
                    }));
        } catch (Throwable e) {
            log.error(e);
        }
        // Storing the second-latest block scanned instead of the first to stop the filter not found error from breaking cron
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_CREATION_L_B_S_.getNameForChain(chainId), latestBlockScanned.get());
    }

    @Override
    public void processBarterTradeEvents(String chainId) {
        Optional<String> lbs = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_TRADE_L_B_S.getNameForChain(chainId));

        AtomicReference<String> latestBlockScanned = lbs.map(AtomicReference::new).orElseGet(() -> new AtomicReference<>(null));

        DefaultBlockParameter defaultBlockParameter = Optional.ofNullable(latestBlockScanned.get()).map(s -> DefaultBlockParameter.valueOf(new BigInteger(s, 16))).orElse(DefaultBlockParameterName.EARLIEST);

        String contractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        SyCrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId, getPrivateKey());

        EthFilter eventFilter = new EthFilter(defaultBlockParameter, DefaultBlockParameterName.LATEST, contractAddress);
        eventFilter.addSingleTopic(BARTER_TRADE_HASH);

        try {
            Flowable<SyCrowBarterFactory_sol_SyCrowBarterFactory.TradeEventResponse> eventFlowable = factory.tradeEventFlowable(eventFilter);
            StreamSupport.stream(eventFlowable.blockingIterable().spliterator(), false)
                    .forEach(((SyCrowBarterFactory_sol_SyCrowBarterFactory.TradeEventResponse eventResponse) -> {
                        String block = eventResponse.log.getBlockNumber().toString(16);
                        latestBlockScanned.set(block);
                        this.processBarterTradeEvent(chainId, eventResponse);
                    }));
        } catch (Throwable e) {
            log.error(e);
        } finally {
            try {
                String latestBlock = web3jForChain(chainId).ethBlockNumber().send().getBlockNumber().toString(16);
                latestBlockScanned.set(latestBlock);
            } catch (Exception ignore) {
            }
        }
        // Storing the second-latest block scanned instead of the first to stop the filter not found error from breaking cron
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_TRADE_L_B_S.getNameForChain(chainId), latestBlockScanned.get());
    }

    @Override
    public void processBarterWithdrawalEvents(String chainId) {
        Optional<String> lbs = platformAttributeHelper.getAttributeValue(PlatformAttributeNamesConstant.BARTER_TOKEN_WITHDRAWAL_L_B_S.getNameForChain(chainId));

        AtomicReference<String> latestBlockScanned = lbs.map(AtomicReference::new).orElseGet(() -> new AtomicReference<>(null));
        DefaultBlockParameter defaultBlockParameter = Optional.ofNullable(latestBlockScanned.get()).map(s -> DefaultBlockParameter.valueOf(new BigInteger(s, 16))).orElse(DefaultBlockParameterName.EARLIEST);

        String contractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        SyCrowBarterFactory_sol_SyCrowBarterFactory factory = this.forChain(chainId, getPrivateKey());

        EthFilter eventFilter = new EthFilter(defaultBlockParameter, DefaultBlockParameterName.LATEST, contractAddress);
        eventFilter.addSingleTopic(BARTER_WITHDRAW_HASH);

        try {
            Flowable<SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse> eventFlowable = factory.completionEventFlowable(eventFilter);
            StreamSupport.stream(eventFlowable.blockingIterable().spliterator(), false)
                    .forEach(((SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse eventResponse) -> {
                        String block = eventResponse.log.getBlockNumber().toString(16);
                        latestBlockScanned.set(block);
                        this.processBarterWithdrawalEvent(chainId, eventResponse);
                    }));
        } catch (Throwable e) {
            log.error(e);
        } finally {
            try {
                String latestBlock = web3jForChain(chainId).ethBlockNumber().send().getBlockNumber().toString(16);
                latestBlockScanned.set(latestBlock);
            } catch (Exception ignore) {
            }
        }
        // Storing the second-latest block scanned instead of the first to stop the filter not found error from breaking cron
        platformAttributeHelper.saveAttribute(PlatformAttributeNamesConstant.BARTER_TOKEN_WITHDRAWAL_L_B_S.getNameForChain(chainId), latestBlockScanned.get());
    }

    @Override
    public BarterSearchResponseModel getBarters(String chainID, BarterFilterModel filterModel) {

        PageRequest pageRequest = PageRequest.of(filterModel.getPage(), filterModel.getLimit(), Sort.by(Sort.Order.desc("dateCreated")));

        Slice<BarterEntity> barterEntityPage;

        if (!Strings.isNullOrEmpty(filterModel.getDepositedTokenAddress()) && !Strings.isNullOrEmpty(filterModel.getExpectsTokenAddress())) {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndDepositTokenContractAndExpectedTokenContractAndStatus(chainID, filterModel.getDepositedTokenAddress().toLowerCase(), filterModel.getExpectsTokenAddress().toLowerCase(), EntityStatusConstant.ACTIVE, pageRequest);
        } else if (!Strings.isNullOrEmpty(filterModel.getDepositedTokenAddress())) {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndDepositTokenContractAndStatus(chainID, filterModel.getDepositedTokenAddress().toLowerCase(), EntityStatusConstant.ACTIVE, pageRequest);
        } else if (!Strings.isNullOrEmpty(filterModel.getExpectsTokenAddress())) {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndExpectedTokenContractAndStatus(chainID, filterModel.getExpectsTokenAddress().toLowerCase(), EntityStatusConstant.ACTIVE, pageRequest);
        } else {
            barterEntityPage = barterEntityRepository.findAllByChainIdAndStatus(chainID, EntityStatusConstant.ACTIVE, pageRequest);
        }

        return BarterSearchResponseModel.builder()
                .hasMore(barterEntityPage.hasNext())
                .barters(barterEntityPage.map(b -> BarterModel.builder().address(b.getBarterContract()).build()).toList())
                .build();
    }

    LocalDateTime fromUTCTimeStampMins(Long timeStamp) {
        return Instant.ofEpochMilli(timeStamp * 1000).atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    String getPrivateKey() {
        String secretKeyName = environment.getProperty(ADMIN_PRIVATE_KEY_NAME_ENV_KEY);
        String secretKeyVersion = environment.getProperty(ADMIN_PRIVATE_KEY_VERSION_ENV_KEY);
        return platformAttributeHelper.getSecretValue(secretKeyName, secretKeyVersion).orElseThrow(() -> new SecretValueNotFoundException(secretKeyName + ":" + secretKeyVersion));
    }

    Web3j web3jForChain(String chainId) {
        return Web3j.build(new HttpService(environment.getProperty(String.format("%s%s", NETWORK_URL_ENV_KEY, chainId))));
    }

    SyCrowBarterFactory_sol_SyCrowBarterFactory forChain(String chainId, String secret) {
        Web3j web3 = web3jForChain(chainId);
        String factoryContractAddress = environment.getProperty(String.format("%s%s", BARTER_TOKEN_FACTORY_ADDRESS_ENV_KEY, chainId));
        Credentials credentials = Credentials.create(secret);
        TransactionManager transactionManager = new RawTransactionManager(web3, credentials, Integer.parseInt(chainId));
        return SyCrowBarterFactory_sol_SyCrowBarterFactory.load(factoryContractAddress, web3, transactionManager, new DefaultGasProvider());
    }

    SyCrowBarter_sol_SyCrowBarter fromContract(String chainId, String contract, String secret) {
        Web3j web3 = web3jForChain(chainId);
        Credentials credentials = Credentials.create(secret);
        TransactionManager transactionManager = new RawTransactionManager(web3, credentials, Integer.parseInt(chainId));
        return SyCrowBarter_sol_SyCrowBarter.load(contract, web3, transactionManager, new DefaultGasProvider());
    }
}

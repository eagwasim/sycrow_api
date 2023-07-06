package com.sycrow.api.service.impl;

import com.sycrow.api.blockchain.SyCrowBarterFactory_sol_SyCrowBarterFactory;
import com.sycrow.api.constant.EntityStatusConstant;
import com.sycrow.api.dto.BarterFilterModel;
import com.sycrow.api.dto.BarterSearchResponseModel;
import com.sycrow.api.model.BarterEntity;
import com.sycrow.api.repository.BarterEntityRepository;
import com.sycrow.api.service.helpers.PlatformAttributeHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BarterServiceImplTest {
    private BarterEntityRepository barterEntityRepository;
    private PlatformAttributeHelper platformAttributeHelper;
    private Environment environment;
    private BarterServiceImpl barterService;

    @BeforeEach
    void setUp() {
        barterEntityRepository = mock(BarterEntityRepository.class);
        platformAttributeHelper = mock(PlatformAttributeHelper.class);
        environment = mock(Environment.class);
        barterService = new BarterServiceImpl(barterEntityRepository, platformAttributeHelper, environment);
    }

    @Test
    void processBarterCreationEvent_ShouldSaveBarterEntity_WhenBarterEntityDoesNotExist() {
        // Arrange
        SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse eventResponse = new SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse();
        eventResponse.log = new org.web3j.protocol.core.methods.response.Log();
        eventResponse.log.setTransactionHash("transaction123");
        eventResponse.barter = "barter123";
        eventResponse.createdBy = "user123";
        eventResponse.deadline = BigInteger.TEN;

        when(barterEntityRepository.findFirstByChainIdAndTransactionId(anyString(), anyString())).thenReturn(Optional.empty());

        // Act
        barterService.processBarterCreationEvent("chain123", eventResponse);

        // Assert
        verify(barterEntityRepository).findFirstByChainIdAndTransactionId("chain123", "transaction123");
        verify(barterEntityRepository).save(any(BarterEntity.class));
    }

    @Test
    void processBarterCreationEvent_ShouldNotSaveBarterEntity_WhenBarterEntityExists() {
        // Arrange
        SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse eventResponse = new SyCrowBarterFactory_sol_SyCrowBarterFactory.CreationEventResponse();
        eventResponse.log = new org.web3j.protocol.core.methods.response.Log();
        eventResponse.log.setTransactionHash("transaction123");
        eventResponse.barter = "barter123";
        eventResponse.createdBy = "user123";
        eventResponse.deadline = BigInteger.TEN;

        when(barterEntityRepository.findFirstByChainIdAndTransactionId(anyString(), anyString())).thenReturn(Optional.of(new BarterEntity()));

        // Act
        barterService.processBarterCreationEvent("chain123", eventResponse);

        // Assert
        verify(barterEntityRepository).findFirstByChainIdAndTransactionId("chain123", "transaction123");
        verify(barterEntityRepository, never()).save(any(BarterEntity.class));
    }

    @Test
    void processBarterTradeEvent_ShouldLogTradeInformation() {
        // Arrange
        SyCrowBarterFactory_sol_SyCrowBarterFactory.TradeEventResponse eventResponse = new SyCrowBarterFactory_sol_SyCrowBarterFactory.TradeEventResponse();
        eventResponse.barter = "barter123";
        eventResponse.inAmount = BigInteger.TEN;
        eventResponse.outAmount = BigInteger.ONE;

        // Act
        barterService.processBarterTradeEvent("chain123", eventResponse);

        // Assert
        // Verify that the trade information is logged (replace with appropriate assertion for your use case)
        // For example: verify(logger).info("New Barter Trade! ----- barter123 ---- 10 ---- 1");
    }

    @Test
    void processBarterWithdrawalEvent_ShouldUpdateBarterEntityStatus_WhenBarterEntityExists() {
        // Arrange
        SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse eventResponse = new SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse();
        eventResponse.barter = "barter123";

        BarterEntity barterEntity = new BarterEntity();
        barterEntity.setBarterContract("barter123");
        barterEntity.setStatus(EntityStatusConstant.ACTIVE);

        when(barterEntityRepository.findFirstByChainIdAndBarterContract(anyString(), anyString())).thenReturn(Optional.of(barterEntity));

        // Act
        barterService.processBarterWithdrawalEvent("chain123", eventResponse);

        // Assert
        verify(barterEntityRepository).findFirstByChainIdAndBarterContract("chain123", "barter123");
        verify(barterEntityRepository).save(barterEntity);
        assertEquals(EntityStatusConstant.COMPLETED, barterEntity.getStatus());
        assertNotNull(barterEntity.getDateModified());
    }

    @Test
    void processBarterWithdrawalEvent_ShouldNotUpdateBarterEntity_WhenBarterEntityDoesNotExist() {
        // Arrange
        SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse eventResponse = new SyCrowBarterFactory_sol_SyCrowBarterFactory.CompletionEventResponse();
        eventResponse.barter = "barter123";

        when(barterEntityRepository.findFirstByChainIdAndBarterContract(anyString(), anyString())).thenReturn(Optional.empty());

        // Act
        barterService.processBarterWithdrawalEvent("chain123", eventResponse);

        // Assert
        verify(barterEntityRepository).findFirstByChainIdAndBarterContract("chain123", "barter123");
        verify(barterEntityRepository, never()).save(any(BarterEntity.class));
    }

    @Test
    void getBarters_ShouldReturnFilteredBarters_WhenFilterModelHasDepositedAndExpectedTokenAddresses() {
        // Arrange
        BarterFilterModel filterModel = new BarterFilterModel();
        filterModel.setDepositedTokenAddress("token123");
        filterModel.setExpectsTokenAddress("token456");

        PageRequest pageRequest = PageRequest.of(filterModel.getPage(), filterModel.getLimit(), Sort.by(Sort.Order.desc("dateCreated")));

        Slice<BarterEntity> expectedPage = mock(Slice.class);
        when(expectedPage.hasNext()).thenReturn(true);
        when(expectedPage.getSize()).thenReturn(1);

        when(barterEntityRepository.findAllByChainIdAndDepositTokenContractAndExpectedTokenContractAndStatus(anyString(), anyString(), anyString(), any(EntityStatusConstant.class), eq(pageRequest)))
                .thenReturn(expectedPage);

        // Act
        BarterSearchResponseModel response = barterService.getBarters("chain123", filterModel);

        // Assert
        verify(barterEntityRepository).findAllByChainIdAndDepositTokenContractAndExpectedTokenContractAndStatus("chain123", "token123", "token456", EntityStatusConstant.ACTIVE, pageRequest);
        assertTrue(response.getHasMore());
        assertEquals(expectedPage.toList().size(), response.getBarters().size());
    }

    // Add more test cases for other methods in BarterServiceImpl

}

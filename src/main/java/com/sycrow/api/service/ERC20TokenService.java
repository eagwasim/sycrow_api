package com.sycrow.api.service;

import com.sycrow.api.dto.CreateERC20TokenModel;
import com.sycrow.api.dto.ERC20TokenModel;
import com.sycrow.api.dto.ERC20TokenSearchResponseModel;

import java.util.List;

public interface ERC20TokenService {
    ERC20TokenSearchResponseModel getActiveTokens(String chainID, String name, int page, int limit);

    ERC20TokenModel getByChainAndContract(String chainId, String contractId);

    ERC20TokenModel createToken(CreateERC20TokenModel model);

    void activateToken(Long tokenID);
}

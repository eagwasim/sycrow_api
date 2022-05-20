package com.sycrow.api.service;

import com.sycrow.api.dto.CreateERC20TokenModel;
import com.sycrow.api.dto.ERC20TokenModel;

import java.util.List;

public interface ERC20TokenService {
    List<ERC20TokenModel> getActiveTokens(String chainID);

    ERC20TokenModel createToken(CreateERC20TokenModel model);

    void activateToken(Long tokenID);
}

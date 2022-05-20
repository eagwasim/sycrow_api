package com.sycrow.api.service;

import com.sycrow.api.dto.ChainModel;
import com.sycrow.api.dto.CreateChainModel;

public interface ChainService {
    ChainModel createChain(CreateChainModel model);
}

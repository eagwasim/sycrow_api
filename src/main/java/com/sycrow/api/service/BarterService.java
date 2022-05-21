package com.sycrow.api.service;

import com.sycrow.api.dto.BarterFilterModel;
import com.sycrow.api.dto.BarterSearchResponseModel;

public interface BarterService {

    void processBarterCreationEvents(String chainId);

    void processBarterTradeEvents(String chainId);

    void processBarterWithdrawalEvents(String chainId);

    BarterSearchResponseModel getBarters(String chainID, BarterFilterModel filterModel);
}

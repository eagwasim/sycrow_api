package com.sycrow.api.dto;

import com.sycrow.api.constant.EntityStatusConstant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChainModel {
    private String chainId;
    private String name;
    private String explorerUrl;
    private String tokenSymbol;

    private EntityStatusConstant status;
}

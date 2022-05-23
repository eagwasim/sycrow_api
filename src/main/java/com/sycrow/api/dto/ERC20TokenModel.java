package com.sycrow.api.dto;

import com.sycrow.api.constant.EntityStatusConstant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ERC20TokenModel {
    private Long id;

    private int priority;
    private Long cmcId;

    private String chainId;

    private String name;
    private String slug;
    private String symbol;
    private String iconUri;
    private String contract;

    private EntityStatusConstant status;

    private Boolean isNative;
    private Boolean isWrapped;
}

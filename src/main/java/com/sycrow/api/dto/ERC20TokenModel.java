package com.sycrow.api.dto;

import com.sycrow.api.constant.EntityStatusConstant;
import lombok.*;

@Data
@Builder
public class ERC20TokenModel {
    private Long id;

    private String chainId;

    private String name;
    private String symbol;
    private String iconUri;
    private String contract;

    private EntityStatusConstant status;

    private Boolean isNative;
    private Boolean isWrapped;
}

package com.sycrow.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BarterFilterModel {
    private String accountAddress;
    private String expectsTokenAddress;
    private String depositedTokenAddress;

    private int page;
    private int limit;
}

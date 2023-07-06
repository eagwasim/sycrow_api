package com.sycrow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarterFilterModel {
    private String accountAddress;
    private String expectsTokenAddress;
    private String depositedTokenAddress;

    private int page;
    private int limit;
}

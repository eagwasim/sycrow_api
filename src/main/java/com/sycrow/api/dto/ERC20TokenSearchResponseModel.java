package com.sycrow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ERC20TokenSearchResponseModel {
    private List<ERC20TokenModel> tokens;
    private Boolean hasMore;
}

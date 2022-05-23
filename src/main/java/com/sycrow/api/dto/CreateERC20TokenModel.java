package com.sycrow.api.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateERC20TokenModel {
    @NotBlank(message = "Pls provide an valid chain id")
    private String chainId;
    @NotBlank(message = "Pls provide a valid name")
    private String name;
    @NotBlank(message = "Pls provide a valid symbol")
    private String symbol;
    @NotBlank(message = "Pls provide a valid symbol")
    private String slug;
    private String iconUri;
    @NotBlank(message = "Pls provide a valid contract address")
    private String contract;
    @NotNull
    private Boolean isNative;
    @NotNull
    private Boolean isWrapped;
    private int priority;
    private Long cmcId;
}

package com.sycrow.api.dto;

import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateChainModel {
    @NotBlank
    private String chainId;
    @NotBlank(message = "INVALID Name")
    private String name;
    @NotBlank(message = "INVALID Explorer")
    private String explorerUrl;
    @NotBlank(message = "INVALID Symbol")
    private String tokenSymbol;
}

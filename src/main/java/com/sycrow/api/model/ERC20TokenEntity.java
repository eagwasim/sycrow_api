package com.sycrow.api.model;

import lombok.*;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.data.annotation.Reference;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "erc20_tokens")
public class ERC20TokenEntity  extends PersistableEntity<Long> {
    @Reference
    private ChainEntity chain;

    private String name;
    private String symbol;
    private String iconUri;
    private String contract;

    private Boolean isNative;
    private Boolean isWrapped;
}

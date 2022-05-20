package com.sycrow.api.model;

import lombok.*;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "chains")
public class ChainEntity extends PersistableEntity<Long> {
    private String chainId;
    private String name;
    private String explorerUrl;
    private String tokenSymbol;
}

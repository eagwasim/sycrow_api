package com.sycrow.api.model;

import lombok.*;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "barters")
public class BarterEntity extends PersistableEntity<Long> {
    private String chainId;

    private String barterContract;
    private String depositTokenContract;
    private String expectedTokenContract;

    private String account;
    private String transactionId;
    private LocalDateTime deadline;
}

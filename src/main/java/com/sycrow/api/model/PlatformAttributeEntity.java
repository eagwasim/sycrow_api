package com.sycrow.api.model;

import lombok.*;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "platform_attributes")
public class PlatformAttributeEntity extends PersistableEntity<Long> {
    private String name;
    private String value;
}

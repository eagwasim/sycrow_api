package com.sycrow.api.model;

import com.sycrow.api.constant.EntityStatusConstant;
import lombok.Data;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
abstract class PersistableEntity<T extends Serializable> {
    @Id
    @Field(name = "entity_identity")
    private T id;

    @CreatedDate
    private LocalDateTime dateCreated;
    @LastModifiedDate
    private LocalDateTime dateModified = LocalDateTime.now();

    private EntityStatusConstant status = EntityStatusConstant.ACTIVE;

}

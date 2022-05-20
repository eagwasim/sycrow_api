package com.sycrow.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskQueueModel<T> {
    T data;
}

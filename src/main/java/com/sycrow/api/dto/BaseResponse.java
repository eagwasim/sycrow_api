package com.sycrow.api.dto;

import lombok.*;

@Data
@Builder
public class BaseResponse <T>{
    private T data;
    private String message;
}

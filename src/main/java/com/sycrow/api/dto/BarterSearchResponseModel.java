package com.sycrow.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BarterSearchResponseModel {
    private List<BarterModel> barters;
    private Long totalCount;
}

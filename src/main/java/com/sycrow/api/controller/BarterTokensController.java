package com.sycrow.api.controller;

import com.sycrow.api.dto.BarterFilterModel;
import com.sycrow.api.dto.BarterSearchResponseModel;
import com.sycrow.api.dto.BaseResponse;
import com.sycrow.api.service.BarterService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Log4j2
@RestController
@RequestMapping(value = "/api/v1/barter/tokens")
public class BarterTokensController {
    private final BarterService barterService;

    public BarterTokensController(BarterService barterService) {
        this.barterService = barterService;
    }

    @GetMapping("/{chainId}")
    public ResponseEntity<?> getBarters(
            @PathVariable("chainId") String chainId,
            @RequestParam("page") @Min(0) int page,
            @RequestParam("limit") @Max(100) @Min(10) int limit,
            @RequestParam("depositTokenContract") @Nullable String depositTokenContract,
            @RequestParam("expectsTokenContract") @Nullable String expectsTokenContract
    ) {
        BarterSearchResponseModel barterSearchResponseModel = barterService.getBarters(chainId, BarterFilterModel.builder()
                .depositedTokenAddress(depositTokenContract)
                .expectsTokenAddress(expectsTokenContract)
                .limit(limit)
                .page(page)
                .build());

        return ResponseEntity.ok(BaseResponse.builder().data(barterSearchResponseModel).build());
    }
}

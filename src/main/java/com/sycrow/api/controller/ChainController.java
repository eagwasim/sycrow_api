package com.sycrow.api.controller;

import com.sycrow.api.dto.BaseResponse;
import com.sycrow.api.dto.ChainModel;
import com.sycrow.api.dto.CreateChainModel;
import com.sycrow.api.service.ChainService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Log4j2
@RestController
@RequestMapping(value = "/api/v1/chains")
public class ChainController {
    private final ChainService chainService;

    public ChainController(ChainService chainService) {
        this.chainService = chainService;
    }

    @PostMapping
    public ResponseEntity<?> createChain(@Valid @RequestBody CreateChainModel model) {
        ChainModel chainModel = chainService.createChain(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.builder().data(chainModel).build());
    }
}

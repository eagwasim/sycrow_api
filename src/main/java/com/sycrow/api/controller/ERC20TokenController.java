package com.sycrow.api.controller;

import com.sycrow.api.dto.BaseResponse;
import com.sycrow.api.dto.CreateERC20TokenModel;
import com.sycrow.api.service.ERC20TokenService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.stream.Collectors;

@Log4j2
@RestController
@RequestMapping(value = "/api/v1/erc20/tokens")
public class ERC20TokenController {
    private final ERC20TokenService erc20TokenService;

    public ERC20TokenController(ERC20TokenService erc20TokenService) {
        this.erc20TokenService = erc20TokenService;
    }

    @PostMapping("/multiple")
    public ResponseEntity<?> createTokens(@RequestBody @Valid CreateERC20TokenModel[] tokens) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .data(Arrays.stream(tokens)
                                .map(erc20TokenService::createToken)
                                .collect(Collectors.toList()))
                        .build());
    }

    @PostMapping
    public ResponseEntity<?> createToken(@RequestBody @Valid CreateERC20TokenModel token) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.builder()
                        .data(erc20TokenService.createToken(token))
                        .build());
    }

    @GetMapping("/{chainId}")
    public ResponseEntity<?> getTokens(@PathVariable("chainId") String chainId, @RequestParam("q") @Nullable String query, @RequestParam("page") int page, @RequestParam("limit") int limit) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.builder()
                        .data(erc20TokenService.getActiveTokens(chainId, query, page, limit))
                        .build());
    }

    @GetMapping("/{chainId}/{contract}")
    public ResponseEntity<?> getTokens(@PathVariable("chainId") String chainId, @PathVariable("contract") String contract) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(BaseResponse.builder()
                        .data(erc20TokenService.getByChainAndContract(chainId, contract))
                        .build());
    }
}

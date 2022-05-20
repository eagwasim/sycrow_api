package com.sycrow.api.controller;

import com.sycrow.api.service.BarterService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Log4j2
@RestController
@RequestMapping(value = "/api/v1/cron")
public class CronController {
    private final BarterService barterService;

    public CronController(BarterService barterService) {
        this.barterService = barterService;
    }

    @GetMapping("/barter/token/events/create/{chainId}")
    ResponseEntity<?> processBarterCreationEvents(@PathVariable("chainId") String chainId, @RequestHeader("User-Agent") String userAgent) {
        if (userAgent == null || !userAgent.equalsIgnoreCase("Google-Cloud-Scheduler")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("lol");
        }
        barterService.processBarterCreationEvents(chainId);
        return ResponseEntity.ok("completed");
    }

    @GetMapping("/barter/token/events/withdraw/{chainId}")
    ResponseEntity<?> processBarterWithdrawalEvents(@PathVariable("chainId") String chainId, @RequestHeader("User-Agent") String userAgent) {
        if (userAgent == null || !userAgent.equalsIgnoreCase("Google-Cloud-Scheduler")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("lol");
        }

        barterService.processBarterWithdrawalEvents(chainId);

        return ResponseEntity.ok("completed");
    }
}

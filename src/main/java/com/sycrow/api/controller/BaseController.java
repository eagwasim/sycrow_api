package com.sycrow.api.controller;

import com.sycrow.api.exception.AuthorizationException;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.Map;

@Log4j2
abstract public class BaseController {
    private static final HashSet<String> SUPPORTED_URLS = new HashSet<>();
    private static final HashSet<String> SUPPORTED_API_KEYS = new HashSet<>();

    static {
        SUPPORTED_URLS.add("http://localhost:4200");
        SUPPORTED_URLS.add("https://sycrow-api.ew.r.appspot.com");
        SUPPORTED_URLS.add("https://sycrow.com");

        SUPPORTED_API_KEYS.add("60af8df1-f8eb-4512-8c7a-36bf933ae836");
    }

    public static void authenticate(Map<String, String> headers) {
        if (!headers.containsKey("X-Forwarded-For".toLowerCase()) && !headers.containsKey("X-SyCrow-Api-Key".toLowerCase())) {
            throw new AuthorizationException();
        }
        if (!SUPPORTED_URLS.contains(headers.get("X-Forwarded-For".toLowerCase())) && !SUPPORTED_API_KEYS.contains(headers.get("X-SyCrow-Api-Key".toLowerCase()))) {
            log.info("Request from unknown source: " + headers.get("X-Forwarded-For".toLowerCase()));
            throw new AuthorizationException();
        }
    }
}

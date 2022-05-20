package com.sycrow.api.exception;

public class SecretValueNotFoundException extends  RuntimeException{
    public SecretValueNotFoundException(String message) {
        super(message);
    }
}

package com.sycrow.api.config;

import com.sycrow.api.dto.BaseResponse;
import com.sycrow.api.exception.AuthorizationException;
import com.sycrow.api.exception.ChainNotSupportedException;
import com.sycrow.api.exception.DuplicateEntityException;
import com.sycrow.api.exception.ERC20TokenNotFoundException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.validation.ConstraintViolationException;

@Log4j2
@ControllerAdvice
public class ErrorConfig {
    @ExceptionHandler(value = {MissingServletRequestParameterException.class, ConstraintViolationException.class})
    public ResponseEntity<?> handleMissingServletRequestParameterException(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(BaseResponse.builder().data(exception.getMessage()).build());
    }

    @ExceptionHandler(value = AuthorizationException.class)
    public ResponseEntity<?> handleAuthorizationException(AuthorizationException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(BaseResponse.builder().data(exception.getMessage()).build());
    }

    @ExceptionHandler(value = ChainNotSupportedException.class)
    public ResponseEntity<?> handleChainNotSupportedException(ChainNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(BaseResponse.builder().data("Chain Not Supported").build());
    }

    @ExceptionHandler(value = DuplicateEntityException.class)
    public ResponseEntity<?> handleDuplicateResourceException(DuplicateEntityException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(BaseResponse.builder().data("Duplicate Resource").build());
    }

    @ExceptionHandler(value = ERC20TokenNotFoundException.class)
    public ResponseEntity<?> handleDuplicateResourceException(ERC20TokenNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(BaseResponse.builder().data("Token not found").build());
    }
}

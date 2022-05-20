package com.sycrow.api.exception;

public class TaskQueueException extends  RuntimeException{
    public TaskQueueException(String message) {
        super(message);
    }
}

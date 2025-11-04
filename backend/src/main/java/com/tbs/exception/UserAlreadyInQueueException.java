package com.tbs.exception;

public class UserAlreadyInQueueException extends RuntimeException {
    public UserAlreadyInQueueException(String message) {
        super(message);
    }
}


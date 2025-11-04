package com.tbs.exception;

public class UserNotInQueueException extends RuntimeException {
    public UserNotInQueueException(String message) {
        super(message);
    }
}


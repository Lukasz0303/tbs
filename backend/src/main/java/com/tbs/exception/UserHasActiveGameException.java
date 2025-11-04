package com.tbs.exception;

public class UserHasActiveGameException extends RuntimeException {
    public UserHasActiveGameException(String message) {
        super(message);
    }
}


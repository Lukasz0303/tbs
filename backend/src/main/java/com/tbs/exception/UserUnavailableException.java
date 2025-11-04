package com.tbs.exception;

public class UserUnavailableException extends RuntimeException {
    public UserUnavailableException(String message) {
        super(message);
    }
}


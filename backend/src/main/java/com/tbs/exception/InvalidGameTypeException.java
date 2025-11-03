package com.tbs.exception;

public class InvalidGameTypeException extends RuntimeException {
    public InvalidGameTypeException(String message) {
        super(message);
    }
}


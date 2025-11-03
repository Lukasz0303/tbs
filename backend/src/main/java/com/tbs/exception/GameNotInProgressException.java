package com.tbs.exception;

public class GameNotInProgressException extends RuntimeException {
    public GameNotInProgressException(String message) {
        super(message);
    }
}


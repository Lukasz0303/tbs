package com.tbs.exception;

public class UserNotInRankingException extends RuntimeException {
    public UserNotInRankingException(String message) {
        super(message);
    }
}


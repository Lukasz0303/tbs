package com.tbs.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long remainingRequests;
    private final long timeToResetSeconds;

    public RateLimitExceededException(String message, long remainingRequests, long timeToResetSeconds) {
        super(message);
        this.remainingRequests = remainingRequests;
        this.timeToResetSeconds = timeToResetSeconds;
    }

    public long getRemainingRequests() {
        return remainingRequests;
    }

    public long getTimeToResetSeconds() {
        return timeToResetSeconds;
    }
}


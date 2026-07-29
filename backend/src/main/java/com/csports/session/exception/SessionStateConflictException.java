package com.csports.session.exception;

public class SessionStateConflictException extends RuntimeException {

    public SessionStateConflictException(String message) {
        super(message);
    }
}

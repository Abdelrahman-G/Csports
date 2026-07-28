package com.csports.session.exception;

public class SessionEndedException extends RuntimeException {

    public SessionEndedException() {
        super("This training session has already ended.");
    }
}
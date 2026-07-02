package com.Csports.Csports.exception;

public class SessionEndedException extends RuntimeException {

    public SessionEndedException() {
        super("This training session has already ended.");
    }
}
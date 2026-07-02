package com.Csports.Csports.exception;

public class SessionFullException extends RuntimeException {

    public SessionFullException() {
        super("This session is already full.");
    }
}
package com.Csports.Csports.exception;

public class SportNotFoundException extends RuntimeException {

    public SportNotFoundException() {
        super("Sport not found.");
    }
}

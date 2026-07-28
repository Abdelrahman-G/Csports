package com.csports.sport.exception;

public class SportNotFoundException extends RuntimeException {

    public SportNotFoundException() {
        super("Sport not found.");
    }
}

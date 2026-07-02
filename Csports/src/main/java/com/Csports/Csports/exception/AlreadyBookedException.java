package com.Csports.Csports.exception;

public class AlreadyBookedException extends RuntimeException {

    public AlreadyBookedException() {
        super("You have already booked this session.");
    }
}
package com.csports.booking.exception;

public class CannotBookOwnSessionException extends RuntimeException {

    public CannotBookOwnSessionException() {
        super("You cannot book your own training session.");
    }
}

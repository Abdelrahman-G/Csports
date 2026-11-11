package com.csports.booking.exception;

public class CannotBookOwnCourseException extends RuntimeException {

    public CannotBookOwnCourseException() {
        super("You cannot book your own training course.");
    }
}

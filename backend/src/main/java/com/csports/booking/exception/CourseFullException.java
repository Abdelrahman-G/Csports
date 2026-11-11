package com.csports.booking.exception;

public class CourseFullException extends RuntimeException {

    public CourseFullException() {
        super("This course is already full.");
    }
}
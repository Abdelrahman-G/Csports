package com.csports.course.exception;

public class CourseEndedException extends RuntimeException {

    public CourseEndedException() {
        super("This training course has already ended.");
    }
}
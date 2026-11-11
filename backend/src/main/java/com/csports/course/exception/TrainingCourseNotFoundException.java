package com.csports.course.exception;

public class TrainingCourseNotFoundException extends RuntimeException {

    public TrainingCourseNotFoundException() {
        super("This training course is no longer available.");
    }

}

package com.Csports.Csports.exception;

public class TrainingSessionNotFoundException extends RuntimeException {

    public TrainingSessionNotFoundException() {
        super("This training session is no longer available.");
    }
    
}

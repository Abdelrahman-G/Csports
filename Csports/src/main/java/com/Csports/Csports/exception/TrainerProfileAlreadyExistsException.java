package com.Csports.Csports.exception;

public class TrainerProfileAlreadyExistsException extends RuntimeException {

    public TrainerProfileAlreadyExistsException() {
        super("Trainer profile already exists.");
    }
}

package com.csports.trainer.exception;

public class TrainerProfileAlreadyExistsException extends RuntimeException {

    public TrainerProfileAlreadyExistsException() {
        super("Trainer profile already exists.");
    }
}

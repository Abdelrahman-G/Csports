package com.csports.trainer.exception;

public class TrainerProfileNotFoundException extends RuntimeException {

    public TrainerProfileNotFoundException() {
        super("Please complete your trainer profile first.");
    }
}
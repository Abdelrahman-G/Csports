package com.csports.common.exception;

public class GeneralException extends RuntimeException {
    public GeneralException() {
        super("Sorry, an unexpected error occurred. Please try again later.");
    }
    
}

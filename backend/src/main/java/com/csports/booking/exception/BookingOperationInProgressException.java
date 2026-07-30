package com.csports.booking.exception;

public class BookingOperationInProgressException extends RuntimeException {

    public BookingOperationInProgressException() {
        super("Another booking operation is in progress for this session. Please retry.");
    }
}

package com.csports.common.validation;

/**
 * Egyptian mobile-number format accepted by the Csports public API.
 */
public final class EgyptianPhoneNumber {

    public static final String REGEX = "^01[0125][0-9]{8}$";
    public static final String MESSAGE =
            "Phone number must be an 11-digit Egyptian mobile number such as 01123456789";

    private EgyptianPhoneNumber() {
    }
}

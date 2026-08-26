package com.csports.common.validation;

/**
 * Gmail address format accepted by the Csports public API.
 */
public final class GmailAddress {

    public static final String REGEX = "(?i)^[a-z0-9._%+-]+@gmail\\.com$";
    public static final String MESSAGE = "Email must be a Gmail address ending in @gmail.com";

    private GmailAddress() {
    }
}

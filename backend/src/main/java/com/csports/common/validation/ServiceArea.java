package com.csports.common.validation;

/**
 * Geographic bounds currently supported by Csports.
 *
 * <p>The box covers the Greater Cairo and Giza service area used by the
 * application's seeded regions. Keeping the values here gives validation,
 * documentation, and future geospatial features one source of truth.</p>
 */
public final class ServiceArea {

    public static final String MIN_LATITUDE = "29.75";
    public static final String MAX_LATITUDE = "30.35";
    public static final String MIN_LONGITUDE = "30.75";
    public static final String MAX_LONGITUDE = "31.75";

    private ServiceArea() {
    }
}

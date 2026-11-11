package com.csports.booking;

import java.util.function.Supplier;

/**
 * Coordinates writes that change the booked-seat count for one course.
 *
 * PostgreSQL constraints and optimistic locking remain the final correctness
 * boundary. This lock reduces avoidable write conflicts across application
 * instances.
 */
public interface CourseBookingLock {

    <T> T execute(Long courseId, Supplier<T> action);
}

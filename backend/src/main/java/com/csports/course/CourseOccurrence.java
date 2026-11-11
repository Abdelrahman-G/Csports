package com.csports.course;

import java.time.ZonedDateTime;

public record CourseOccurrence(ZonedDateTime startsAt, ZonedDateTime endsAt) {}

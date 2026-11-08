package com.csports.session;

import java.time.ZonedDateTime;

public record CourseOccurrence(ZonedDateTime startsAt, ZonedDateTime endsAt) {}

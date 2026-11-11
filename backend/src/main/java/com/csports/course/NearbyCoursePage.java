package com.csports.course;

import java.util.List;

record NearbyCoursePage(List<NearbyCourseMatch> matches, long totalElements) {
}

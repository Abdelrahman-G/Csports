package com.csports.session;

import java.util.List;

record NearbySessionPage(List<NearbySessionMatch> matches, long totalElements) {
}

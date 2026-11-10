package com.csports.announcement;

import com.csports.common.pagination.PageResponse;

public record NoticeboardResponse(boolean canPublish, PageResponse<AnnouncementResponse> announcements) {}

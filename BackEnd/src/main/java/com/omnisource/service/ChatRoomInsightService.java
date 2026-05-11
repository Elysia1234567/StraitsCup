package com.omnisource.service;

import com.omnisource.dto.response.ChatRoomInsightResponse;

public interface ChatRoomInsightService {
    ChatRoomInsightResponse getRoomInsight(Long roomId);
}

package com.omnisource.service;

import com.omnisource.entity.ChatRoom;
import com.omnisource.entity.ChatRoomMember;

import java.util.List;

public interface ChatRoomService {
    ChatRoom createRoom(Long userId, String name, Long themeId, List<String> agentCodes);
    List<ChatRoom> getUserRooms(Long userId);
    ChatRoom getRoomById(Long roomId);
    void dissolveRoom(Long roomId, Long userId);
    List<ChatRoomMember> getRoomMembers(Long roomId);
    List<ChatRoomMember> getRoomAgentMembers(Long roomId);
    ChatRoomMember replaceRoomAgent(Long roomId, Long memberId, String agentCode);
    void removeRoomAgent(Long roomId, Long memberId);
}

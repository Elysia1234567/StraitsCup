package com.omnisource.controller;

import com.omnisource.entity.ChatMessage;
import com.omnisource.entity.ChatRoom;
import com.omnisource.entity.ChatRoomMember;
import com.omnisource.service.ChatHistoryService;
import com.omnisource.service.ChatRoomService;
import com.omnisource.utils.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private static final Long LOCAL_USER_ID = 1L;

    private final ChatRoomService chatRoomService;
    private final ChatHistoryService chatHistoryService;

    @GetMapping
    public Result<List<ChatRoom>> getMyRooms() {
        Long userId = getUserId();
        return Result.success(chatRoomService.getUserRooms(userId));
    }

    @PostMapping
    public Result<ChatRoom> createRoom(@RequestBody Map<String, Object> request) {
        Long userId = getUserId();
        String name = (String) request.get("name");
        Long themeId = request.get("themeId") != null ? Long.valueOf(request.get("themeId").toString()) : null;
        @SuppressWarnings("unchecked")
        List<String> agentCodes = (List<String>) request.get("agentCodes");

        ChatRoom room = chatRoomService.createRoom(userId, name, themeId, agentCodes);
        return Result.success(room);
    }

    @GetMapping("/{roomId}")
    public Result<ChatRoom> getRoom(@PathVariable Long roomId) {
        return Result.success(chatRoomService.getRoomById(roomId));
    }

    @GetMapping("/{roomId}/agents")
    public Result<List<ChatRoomMember>> getRoomAgents(@PathVariable Long roomId) {
        return Result.success(chatRoomService.getRoomAgentMembers(roomId));
    }

    @PutMapping("/{roomId}/agents/{memberId}")
    public Result<ChatRoomMember> replaceRoomAgent(
            @PathVariable Long roomId,
            @PathVariable Long memberId,
            @RequestBody Map<String, Object> request) {
        String agentCode = (String) request.get("agentCode");
        try {
            return Result.success(chatRoomService.replaceRoomAgent(roomId, memberId, agentCode));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/{roomId}/agents/{memberId}")
    public Result<Void> removeRoomAgent(@PathVariable Long roomId, @PathVariable Long memberId) {
        try {
            chatRoomService.removeRoomAgent(roomId, memberId);
            return Result.success();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.badRequest(e.getMessage());
        }
    }

    @DeleteMapping("/{roomId}")
    public Result<Void> dissolveRoom(@PathVariable Long roomId) {
        Long userId = getUserId();
        chatRoomService.dissolveRoom(roomId, userId);
        return Result.success();
    }

    private Long getUserId() {
        return LOCAL_USER_ID;
    }

    @GetMapping("/{roomId}/messages")
    public Result<List<ChatMessage>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(chatHistoryService.getHistoryPage(roomId, page, size));
    }

    @GetMapping("/{roomId}/messages/recent")
    public Result<List<ChatMessage>> getRecentMessages(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(chatHistoryService.getRecentHistory(roomId, limit));
    }
}

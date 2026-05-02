package com.omnisource.service.impl;

import com.omnisource.Agents.AgentDefaults;
import com.omnisource.entity.Agent;
import com.omnisource.entity.ChatRoom;
import com.omnisource.entity.ChatRoomMember;
import com.omnisource.mapper.ChatRoomMapper;
import com.omnisource.service.AgentService;
import com.omnisource.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final AgentService agentService;

    /**
     * 创建聊天室并写入初始成员。
     *
     * @param userId 创建聊天室的用户 ID
     * @param name 聊天室名称
     * @param themeId 文物或文化主题 ID，可以为空
     * @param agentCodes 前端指定加入房间的 Agent 编码，为空时加入默认器灵
     * @return 创建完成的聊天室实体
     */
    @Override
    @Transactional
    public ChatRoom createRoom(Long userId, String name, Long themeId, List<String> agentCodes) {
        List<String> selectedAgentCodes = CollectionUtils.isEmpty(agentCodes)
                ? AgentDefaults.DEFAULT_AGENT_CODES
                : new ArrayList<>(new LinkedHashSet<>(agentCodes));

        ChatRoom room = new ChatRoom();
        room.setRoomCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        room.setUserId(userId);
        room.setThemeId(themeId);
        room.setName(name);
        room.setMaxMembers(7);
        room.setMemberCount(1);
        room.setMessageCount(0);
        room.setStatus(1);

        chatRoomMapper.insert(room);

        ChatRoomMember userMember = new ChatRoomMember();
        userMember.setRoomId(room.getId());
        userMember.setMemberType("USER");
        userMember.setUserId(userId);
        userMember.setDisplayName("用户" + userId);
        userMember.setRoleInRoom("CREATOR");
        chatRoomMapper.insertMember(userMember);

        for (String code : selectedAgentCodes) {
            Agent agent = agentService.getAgentByCode(code);
            if (agent != null) {
                ChatRoomMember agentMember = new ChatRoomMember();
                agentMember.setRoomId(room.getId());
                agentMember.setMemberType("AGENT");
                agentMember.setAgentId(agent.getId());
                agentMember.setDisplayName(agent.getName());
                agentMember.setAvatar(agent.getAvatar());
                agentMember.setRoleInRoom("MEMBER");
                chatRoomMapper.insertMember(agentMember);
                room.setMemberCount(room.getMemberCount() + 1);
            }
        }

        chatRoomMapper.updateMemberCount(room.getId(), room.getMemberCount());

        return room;
    }

    /**
     * 查询某个用户创建或参与的聊天室。
     *
     * @param userId 用户 ID
     * @return 聊天室列表
     */
    @Override
    public List<ChatRoom> getUserRooms(Long userId) {
        return chatRoomMapper.selectByUserId(userId);
    }

    /**
     * 根据房间 ID 查询聊天室。
     *
     * @param roomId 聊天室 ID
     * @return 聊天室实体，不存在时返回 null
     */
    @Override
    public ChatRoom getRoomById(Long roomId) {
        return chatRoomMapper.selectById(roomId);
    }

    /**
     * 解散聊天室。
     *
     * @param roomId 聊天室 ID
     * @param userId 当前操作用户 ID，只有创建者可以解散
     */
    @Override
    @Transactional
    public void dissolveRoom(Long roomId, Long userId) {
        ChatRoom room = chatRoomMapper.selectById(roomId);
        if (room == null || !room.getUserId().equals(userId)) {
            return;
        }
        chatRoomMapper.deleteMembersByRoomId(roomId);
        chatRoomMapper.softDelete(roomId);
    }

    /**
     * 查询聊天室全部成员。
     *
     * @param roomId 聊天室 ID
     * @return 成员列表，包含用户和 Agent
     */
    @Override
    public List<ChatRoomMember> getRoomMembers(Long roomId) {
        return chatRoomMapper.selectMembersByRoomId(roomId);
    }

    /**
     * 查询聊天室中的 Agent 成员。
     *
     * @param roomId 聊天室 ID
     * @return Agent 成员列表
     */
    @Override
    public List<ChatRoomMember> getRoomAgentMembers(Long roomId) {
        return chatRoomMapper.selectAgentMembersByRoomId(roomId);
    }

    @Override
    @Transactional
    public ChatRoomMember replaceRoomAgent(Long roomId, Long memberId, String agentCode) {
        ChatRoomMember member = chatRoomMapper.selectMemberById(memberId);
        if (member == null || !roomId.equals(member.getRoomId()) || !"AGENT".equals(member.getMemberType())) {
            throw new IllegalArgumentException("Agent 成员不存在");
        }

        Agent agent = agentService.getAgentByCode(agentCode);
        if (agent == null) {
            throw new IllegalArgumentException("Agent 不存在");
        }
        boolean alreadyInRoom = chatRoomMapper.selectAgentMembersByRoomId(roomId).stream()
                .anyMatch(existing -> !existing.getId().equals(memberId) && agent.getId().equals(existing.getAgentId()));
        if (alreadyInRoom) {
            throw new IllegalArgumentException("该 Agent 已在聊天室中");
        }

        member.setAgentId(agent.getId());
        member.setDisplayName(agent.getName());
        member.setAvatar(agent.getAvatar());
        chatRoomMapper.updateAgentMember(member);
        return chatRoomMapper.selectMemberById(memberId);
    }

    @Override
    @Transactional
    public void removeRoomAgent(Long roomId, Long memberId) {
        ChatRoomMember member = chatRoomMapper.selectMemberById(memberId);
        if (member == null || !roomId.equals(member.getRoomId()) || !"AGENT".equals(member.getMemberType())) {
            throw new IllegalArgumentException("Agent 成员不存在");
        }

        int agentCount = chatRoomMapper.countAgentMembersByRoomId(roomId);
        if (agentCount <= 1) {
            throw new IllegalStateException("聊天室至少保留一个 Agent");
        }

        chatRoomMapper.deleteAgentMember(roomId, memberId);
        chatRoomMapper.updateMemberCount(roomId, agentCount);
    }
}

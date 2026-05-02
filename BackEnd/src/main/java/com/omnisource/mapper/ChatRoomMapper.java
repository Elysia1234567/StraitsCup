package com.omnisource.mapper;

import com.omnisource.entity.ChatRoom;
import com.omnisource.entity.ChatRoomMember;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatRoomMapper {

    @Select("SELECT * FROM chat_room WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY create_time DESC")
    List<ChatRoom> selectByUserId(Long userId);

    @Select("SELECT * FROM chat_room WHERE id = #{id} AND is_deleted = 0 LIMIT 1")
    ChatRoom selectById(Long id);

    @Select("SELECT * FROM chat_room WHERE room_code = #{roomCode} AND is_deleted = 0 LIMIT 1")
    ChatRoom selectByRoomCode(String roomCode);

    @Insert("INSERT INTO chat_room (room_code, user_id, theme_id, name, description, max_members, member_count, message_count, status) " +
            "VALUES (#{roomCode}, #{userId}, #{themeId}, #{name}, #{description}, #{maxMembers}, #{memberCount}, #{messageCount}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatRoom chatRoom);

    @Update("UPDATE chat_room SET name = #{name}, description = #{description}, status = #{status}, " +
            "member_count = #{memberCount}, message_count = #{messageCount} " +
            "WHERE id = #{id}")
    int update(ChatRoom chatRoom);

    @Update("UPDATE chat_room SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(Long id);

    @Select("SELECT COUNT(*) FROM chat_room WHERE user_id = #{userId} AND is_deleted = 0")
    int countByUserId(Long userId);

    @Insert("INSERT INTO chat_room_member (room_id, member_type, user_id, agent_id, display_name, avatar, role_in_room) " +
            "VALUES (#{roomId}, #{memberType}, #{userId}, #{agentId}, #{displayName}, #{avatar}, #{roleInRoom})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMember(ChatRoomMember member);

    @Select("SELECT * FROM chat_room_member WHERE room_id = #{roomId} AND is_deleted = 0 ORDER BY join_time")
    List<ChatRoomMember> selectMembersByRoomId(Long roomId);

    @Select("SELECT * FROM chat_room_member WHERE room_id = #{roomId} AND member_type = 'AGENT' AND is_deleted = 0 ORDER BY join_time, id")
    List<ChatRoomMember> selectAgentMembersByRoomId(Long roomId);

    @Select("SELECT * FROM chat_room_member WHERE id = #{id} AND is_deleted = 0 LIMIT 1")
    ChatRoomMember selectMemberById(Long id);

    @Select("SELECT COUNT(*) FROM chat_room_member WHERE room_id = #{roomId} AND member_type = 'AGENT' AND is_deleted = 0")
    int countAgentMembersByRoomId(Long roomId);

    @Update("UPDATE chat_room_member SET agent_id = #{agentId}, display_name = #{displayName}, avatar = #{avatar}, " +
            "update_time = NOW() WHERE id = #{id} AND room_id = #{roomId} AND member_type = 'AGENT' AND is_deleted = 0")
    int updateAgentMember(ChatRoomMember member);

    @Update("UPDATE chat_room_member SET status = 0, is_deleted = 1 WHERE id = #{memberId} AND room_id = #{roomId} " +
            "AND member_type = 'AGENT' AND is_deleted = 0")
    int deleteAgentMember(@Param("roomId") Long roomId, @Param("memberId") Long memberId);

    @Update("UPDATE chat_room SET member_count = #{memberCount} WHERE id = #{roomId}")
    int updateMemberCount(@Param("roomId") Long roomId, @Param("memberCount") int memberCount);

    @Update("UPDATE chat_room_member SET status = 0, is_deleted = 1 WHERE room_id = #{roomId}")
    int deleteMembersByRoomId(Long roomId);

    @Update("UPDATE chat_room_member SET last_speak_time = #{lastSpeakTime}, speak_count = #{speakCount} " +
            "WHERE id = #{id}")
    int updateMemberSpeakInfo(ChatRoomMember member);
}

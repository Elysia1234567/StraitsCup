package com.omnisource.mapper;

import com.omnisource.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (room_id, message_type, sender_type, sender_id, sender_name, " +
            "sender_avatar, content, image_url, reply_to_message_id, metadata, is_stream, stream_id, " +
            "search_enabled, search_results) " +
            "VALUES (#{roomId}, #{messageType}, #{senderType}, #{senderId}, #{senderName}, " +
            "#{senderAvatar}, #{content}, #{imageUrl}, #{replyToMessageId}, #{metadata}, #{isStream}, #{streamId}, " +
            "#{searchEnabled}, #{searchResults})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("SELECT * FROM chat_message WHERE room_id = #{roomId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{limit}")
    List<ChatMessage> selectRecentByRoomId(@Param("roomId") Long roomId, @Param("limit") int limit);

    @Select("SELECT * FROM chat_message WHERE room_id = #{roomId} AND is_deleted = 0 ORDER BY create_time DESC LIMIT #{offset}, #{size}")
    List<ChatMessage> selectByRoomIdWithPage(@Param("roomId") Long roomId, @Param("offset") int offset, @Param("size") int size);

    @Select("SELECT COUNT(*) FROM chat_message WHERE room_id = #{roomId} AND is_deleted = 0")
    int countByRoomId(Long roomId);

    @Update("UPDATE chat_message SET content = #{content} WHERE id = #{id}")
    int updateContent(@Param("id") Long id, @Param("content") String content);

    @Update("UPDATE chat_message SET is_deleted = 1 WHERE room_id = #{roomId}")
    int deleteByRoomId(Long roomId);
}

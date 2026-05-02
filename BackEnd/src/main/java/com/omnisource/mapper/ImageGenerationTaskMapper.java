package com.omnisource.mapper;

import com.omnisource.entity.ImageGenerationTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ImageGenerationTaskMapper {

    @Insert("INSERT INTO image_generation_task (task_id, user_id, room_id, prompt, style, status, progress, model) " +
            "VALUES (#{taskId}, #{userId}, #{roomId}, #{prompt}, #{style}, #{status}, #{progress}, #{model})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ImageGenerationTask task);

    @Select("SELECT * FROM image_generation_task WHERE task_id = #{taskId} LIMIT 1")
    ImageGenerationTask selectByTaskId(String taskId);

    @Select("SELECT * FROM image_generation_task WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 20")
    List<ImageGenerationTask> selectByUserId(Long userId);

    @Update("UPDATE image_generation_task SET status = #{status}, progress = #{progress}, " +
            "result_url = #{resultUrl}, error_message = #{errorMessage} " +
            "WHERE task_id = #{taskId}")
    int updateStatus(ImageGenerationTask task);
}

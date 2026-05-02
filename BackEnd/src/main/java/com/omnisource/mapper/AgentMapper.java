package com.omnisource.mapper;

import com.omnisource.entity.Agent;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AgentMapper {

    @Select("SELECT * FROM agent WHERE is_deleted = 0 AND status = 1 ORDER BY sort_order")
    List<Agent> selectAllActive();

    @Select("SELECT * FROM agent WHERE agent_code = #{agentCode} AND is_deleted = 0 LIMIT 1")
    Agent selectByCode(String agentCode);

    @Select("SELECT * FROM agent WHERE agent_code = #{agentCode} LIMIT 1")
    Agent selectAnyByCode(String agentCode);

    @Select("SELECT * FROM agent WHERE id = #{id} AND is_deleted = 0 LIMIT 1")
    Agent selectById(Long id);

    @Insert("INSERT INTO agent (agent_code, name, avatar, role_type, personality, prompt_template, " +
            "knowledge_scope, language_style, constraints, max_tokens, temperature, top_p, " +
            "is_preset, sort_order, status) " +
            "VALUES (#{agentCode}, #{name}, #{avatar}, #{roleType}, #{personality}, #{promptTemplate}, " +
            "#{knowledgeScope}, #{languageStyle}, #{constraints}, #{maxTokens}, #{temperature}, #{topP}, " +
            "#{isPreset}, #{sortOrder}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Agent agent);

    @Update("UPDATE agent SET name = #{name}, avatar = #{avatar}, role_type = #{roleType}, personality = #{personality}, " +
            "prompt_template = #{promptTemplate}, knowledge_scope = #{knowledgeScope}, " +
            "language_style = #{languageStyle}, constraints = #{constraints}, " +
            "max_tokens = #{maxTokens}, temperature = #{temperature}, top_p = #{topP}, " +
            "is_preset = #{isPreset}, sort_order = #{sortOrder}, status = #{status}, is_deleted = 0 " +
            "WHERE id = #{id}")
    int update(Agent agent);

    @Update("UPDATE agent SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(Long id);
}

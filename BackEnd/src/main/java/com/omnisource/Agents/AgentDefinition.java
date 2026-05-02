package com.omnisource.Agents;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Agent 静态定义模型。
 *
 * <p>功能：用代码描述一个具体 Agent 的基础配置，方便和数据库初始化数据保持一致。
 * 运行时仍然从数据库读取 Agent，这个类主要用于集中管理和后续扩展参考。</p>
 */
@Value
@Builder
public class AgentDefinition {

    /** Agent 唯一编码，对应数据库 agent.agent_code。 */
    String agentCode;

    /** Agent 显示名称。 */
    String name;

    /** Agent 头像地址。 */
    String avatar;

    /** Agent 类型，例如 ARTIFACT_SPIRIT 或 IMMERSIVE_SPIRIT。 */
    String roleType;

    /** Agent 性格摘要。 */
    String personality;

    /** Agent 核心提示词模板。 */
    String promptTemplate;

    /** Agent 知识范围或模式标记。 */
    String knowledgeScope;

    /** Agent 语言风格。 */
    String languageStyle;

    /** Agent 回复约束。 */
    String constraints;

    /** 单次回复建议最大字数。 */
    Integer maxTokens;

    /** 创造性参数，值越高表达越发散。 */
    BigDecimal temperature;

    /** 采样参数，控制可选词范围。 */
    BigDecimal topP;

    /** 排序值，越小越靠前。 */
    Integer sortOrder;
}

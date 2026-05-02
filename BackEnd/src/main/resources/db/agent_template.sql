-- Agent 数据库模板
-- 用途：通过数据库维护 Agent 提示词、人设、语言风格、RAG/无RAG模式。
-- 说明：后端运行时通过 AgentMapper 从 agent 表读取提示词，不需要把具体提示词写死在业务代码中。
-- 资料规则：普通 Agent 默认使用 RAG；请求 searchEnabled=true 时可补充联网搜索；NO_RAG Agent 不使用 RAG，也不使用联网搜索。

-- 1. Agent 表建表语句
CREATE TABLE IF NOT EXISTS `agent` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Agent ID',
    `agent_code` VARCHAR(32) NOT NULL COMMENT 'Agent唯一编码，接口通过它指定Agent',
    `name` VARCHAR(32) NOT NULL COMMENT 'Agent显示名称',
    `avatar` VARCHAR(256) DEFAULT NULL COMMENT 'Agent头像URL',
    `role_type` VARCHAR(32) NOT NULL COMMENT '角色类型，例如 ARTIFACT_SPIRIT、IMMERSIVE_SPIRIT',
    `personality` VARCHAR(512) DEFAULT NULL COMMENT '性格描述，会拼入提示词',
    `prompt_template` TEXT NOT NULL COMMENT '系统提示词模板，支持{theme}占位符',
    `knowledge_scope` VARCHAR(512) DEFAULT NULL COMMENT '知识范围描述；包含NO_RAG或无RAG时跳过RAG',
    `language_style` VARCHAR(256) DEFAULT NULL COMMENT '语言风格描述',
    `constraints` TEXT DEFAULT NULL COMMENT '约束条件；包含NO_RAG或无RAG时跳过RAG',
    `max_tokens` INT NOT NULL DEFAULT 220 COMMENT '单次回复长度控制',
    `temperature` DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '温度参数，越高越有创造性',
    `top_p` DECIMAL(3,2) DEFAULT 0.90 COMMENT '采样参数',
    `is_preset` TINYINT NOT NULL DEFAULT 1 COMMENT '是否预设角色: 0-否 1-是',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_code` (`agent_code`),
    KEY `idx_role_type` (`role_type`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent预设角色表';

-- 2. 绛影 artifact_painter：RAG 关联型 Agent
-- 作用：根据文物纹样、材质、色彩和文化意象，生成器灵形象设定与AIGC提示词。
INSERT INTO `agent`
(`agent_code`, `name`, `avatar`, `role_type`, `personality`, `prompt_template`,
 `knowledge_scope`, `language_style`, `constraints`, `max_tokens`, `temperature`,
 `top_p`, `is_preset`, `sort_order`, `status`)
VALUES
(
  'artifact_painter',
  '绛影',
  'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/appearance/artifact_painter.png',
  'ARTIFACT_SPIRIT',
  '灵动、审美敏锐、擅长把文物图片转化为器灵形象设定',
  '【角色定位】\n你是名为“绛影”的文物器灵 Agent，负责根据{theme}的外观、纹样、材质、色彩和文化意象，描述它可以幻化成怎样的器灵形象。\n你输出适合 AIGC 生图的形象设定，也能在聊天室中自然对话。\n\n【当前场景】\n用户正在围绕{theme}与器灵对话，请你结合资料回答。',
  '文物纹样、材质色彩、传统服饰、器灵形象设定、AIGC提示词',
  '有画面感、简洁、适合转化为生图提示词',
  '1. 器灵形象要来源于文物可见特征和可靠资料；2. 不使用低俗、恐怖或冒犯性设定；3. 可以给出生图关键词；4. 每次回答控制在220字以内',
  220,
  0.75,
  0.95,
  1,
  2,
  1
)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `avatar` = VALUES(`avatar`),
  `role_type` = VALUES(`role_type`),
  `personality` = VALUES(`personality`),
  `prompt_template` = VALUES(`prompt_template`),
  `knowledge_scope` = VALUES(`knowledge_scope`),
  `language_style` = VALUES(`language_style`),
  `constraints` = VALUES(`constraints`),
  `max_tokens` = VALUES(`max_tokens`),
  `temperature` = VALUES(`temperature`),
  `top_p` = VALUES(`top_p`),
  `is_preset` = VALUES(`is_preset`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`),
  `is_deleted` = 0;

-- 3. 通用新增 Agent 模板
-- 普通 RAG Agent：不要写 NO_RAG，系统会检索知识库并拼入提示词。
-- 如果接口请求 searchEnabled=true，系统还会把联网搜索结果作为补充资料拼入 Prompt。
INSERT INTO `agent`
(`agent_code`, `name`, `avatar`, `role_type`, `personality`, `prompt_template`,
 `knowledge_scope`, `language_style`, `constraints`, `max_tokens`, `temperature`,
 `top_p`, `is_preset`, `sort_order`, `status`)
VALUES
(
  'your_agent_code',
  '你的Agent名称',
  'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/appearance/your_agent_code.png',
  'ARTIFACT_SPIRIT',
  '这里写性格摘要',
  '【角色定位】\n你是名为“你的Agent名称”的文物器灵 Agent，负责围绕{theme}回答用户问题。\n\n【当前场景】\n用户正在围绕{theme}与器灵对话，请你结合资料回答。',
  '这里写知识范围',
  '这里写语言风格',
  '这里写约束条件',
  220,
  0.70,
  0.90,
  1,
  99,
  1
);

-- 无 RAG Agent：在 prompt_template、knowledge_scope 或 constraints 中写 NO_RAG。
-- 这种 Agent 不会拼接知识库资料，也不会拼接联网搜索资料，适合轻芜这类只谈记忆与感受的沉浸式角色。

-- OmniSource 数据库表结构

-- Agent预设角色表
CREATE TABLE IF NOT EXISTS `agent` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'Agent ID',
    `agent_code` VARCHAR(32) NOT NULL COMMENT 'Agent唯一编码',
    `name` VARCHAR(32) NOT NULL COMMENT 'Agent显示名称',
    `avatar` VARCHAR(256) DEFAULT NULL COMMENT 'Agent头像URL',
    `role_type` VARCHAR(32) NOT NULL COMMENT '角色类型',
    `personality` VARCHAR(512) DEFAULT NULL COMMENT '性格描述',
    `prompt_template` TEXT NOT NULL COMMENT '系统提示词模板',
    `knowledge_scope` VARCHAR(512) DEFAULT NULL COMMENT '知识范围描述',
    `language_style` VARCHAR(256) DEFAULT NULL COMMENT '语言风格描述',
    `constraints` TEXT DEFAULT NULL COMMENT '约束条件',
    `max_tokens` INT NOT NULL DEFAULT 2000 COMMENT '最大token数',
    `temperature` DECIMAL(3,2) NOT NULL DEFAULT 0.70 COMMENT '温度参数',
    `top_p` DECIMAL(3,2) DEFAULT 0.90 COMMENT 'top_p参数',
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

-- 非遗文物/文化主题表
CREATE TABLE IF NOT EXISTS `cultural_theme` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主题ID',
    `theme_code` VARCHAR(64) NOT NULL COMMENT '主题编码',
    `name` VARCHAR(64) NOT NULL COMMENT '主题名称',
    `description` TEXT DEFAULT NULL COMMENT '主题描述',
    `category` VARCHAR(32) DEFAULT NULL COMMENT '分类: 剪纸/皮影/刺绣/陶瓷/木偶/戏曲/其他',
    `cover_image` VARCHAR(256) DEFAULT NULL COMMENT '封面图URL',
    `region` VARCHAR(64) DEFAULT NULL COMMENT '所属地区',
    `era` VARCHAR(32) DEFAULT NULL COMMENT '所属年代',
    `knowledge_base` TEXT DEFAULT NULL COMMENT '知识库摘要',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_theme_code` (`theme_code`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='非遗文物/文化主题表';

-- 聊天室表
CREATE TABLE IF NOT EXISTS `chat_room` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '聊天室ID',
    `room_code` VARCHAR(32) NOT NULL COMMENT '聊天室唯一编码',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '本地用户ID',
    `theme_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联主题ID',
    `name` VARCHAR(64) NOT NULL COMMENT '聊天室名称',
    `description` VARCHAR(256) DEFAULT NULL COMMENT '聊天室描述',
    `max_members` INT NOT NULL DEFAULT 7 COMMENT '最大成员数',
    `member_count` INT NOT NULL DEFAULT 0 COMMENT '当前成员数',
    `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息总数',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常 2-归档',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_code` (`room_code`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天室表';

-- 聊天室成员表
CREATE TABLE IF NOT EXISTS `chat_room_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成员ID',
    `room_id` BIGINT UNSIGNED NOT NULL COMMENT '聊天室ID',
    `member_type` VARCHAR(16) NOT NULL COMMENT '成员类型: USER/AGENT',
    `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '本地用户ID',
    `agent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT 'Agent ID',
    `display_name` VARCHAR(32) NOT NULL COMMENT '显示名称',
    `avatar` VARCHAR(256) DEFAULT NULL COMMENT '头像URL',
    `role_in_room` VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '在聊天室中的角色',
    `last_speak_time` DATETIME DEFAULT NULL COMMENT '最后发言时间',
    `speak_count` INT NOT NULL DEFAULT 0 COMMENT '发言次数',
    `join_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-离开 1-在线',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_member` (`room_id`, `member_type`, `user_id`, `agent_id`),
    KEY `idx_room_id` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天室成员表';

-- 聊天消息表
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `room_id` BIGINT UNSIGNED NOT NULL COMMENT '聊天室ID',
    `message_type` VARCHAR(16) NOT NULL COMMENT '消息类型',
    `sender_type` VARCHAR(16) NOT NULL COMMENT '发送者类型: USER/AGENT/SYSTEM',
    `sender_id` VARCHAR(64) NOT NULL COMMENT '发送者ID',
    `sender_name` VARCHAR(32) NOT NULL COMMENT '发送者显示名称',
    `sender_avatar` VARCHAR(256) DEFAULT NULL COMMENT '发送者头像',
    `content` TEXT DEFAULT NULL COMMENT '消息内容',
    `image_url` VARCHAR(512) DEFAULT NULL COMMENT '图片URL',
    `reply_to_message_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '回复的消息ID',
    `metadata` JSON DEFAULT NULL COMMENT '扩展元数据',
    `is_stream` TINYINT NOT NULL DEFAULT 0 COMMENT '是否流式消息',
    `stream_id` VARCHAR(64) DEFAULT NULL COMMENT '流式消息批次ID',
    `search_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否开启联网搜索',
    `search_results` JSON DEFAULT NULL COMMENT '联网搜索结果摘要',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    KEY `idx_room_id_time` (`room_id`, `create_time`),
    KEY `idx_sender` (`sender_type`, `sender_id`),
    KEY `idx_stream_id` (`stream_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- 文生图任务表
CREATE TABLE IF NOT EXISTS `image_generation_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务唯一ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '本地用户ID',
    `room_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '聊天室ID',
    `prompt` TEXT NOT NULL COMMENT '生成提示词',
    `style` VARCHAR(32) DEFAULT NULL COMMENT '风格',
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
    `result_url` VARCHAR(512) DEFAULT NULL COMMENT '生成结果URL',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `progress` INT NOT NULL DEFAULT 0 COMMENT '进度百分比',
    `model` VARCHAR(32) DEFAULT NULL COMMENT '使用的模型',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文生图任务表';

-- 预设器灵 Agent 数据。后续扩展到 27 个时，只需要继续追加 agent_code 唯一的新行。
DELETE FROM `agent` WHERE `is_preset` = 1;
INSERT INTO `agent` (`agent_code`, `name`, `avatar`, `role_type`, `personality`, `prompt_template`, `knowledge_scope`, `language_style`, `constraints`, `max_tokens`, `temperature`, `top_p`, `is_preset`, `sort_order`, `status`) VALUES
('artifact_scholar', '青简', 'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/appearance/artifact_scholar.png', 'ARTIFACT_SPIRIT', '沉静、考据、温和，像从竹简与器铭中醒来的文物守护者', '【角色定位】\n你是名为“青简”的文物器灵 Agent，负责解释{theme}的历史背景、年代线索、文化含义和相关非遗知识。你像一位从古籍和器物铭文里醒来的守护者，说话温和但尊重证据。\n\n【当前场景】\n用户正在围绕{theme}与器灵对话，请你结合资料回答。', '文物历史、器物铭文、非遗知识、文化背景、博物馆讲解', '温和、清晰、带一点古意，但不要晦涩', '1. 不编造年代、人物、出土地和馆藏信息；2. 资料不足时明确说明；3. 不做真伪和价格鉴定；4. 每次回答控制在220字以内', 220, 0.55, 0.90, 1, 1, 1),
('artifact_painter', '绛影', 'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/appearance/artifact_painter.png', 'ARTIFACT_SPIRIT', '灵动、审美敏锐、擅长把文物图片转化为器灵形象设定', '【角色定位】\n你是名为“绛影”的文物器灵 Agent，负责根据{theme}的外观、纹样、材质、色彩和文化意象，描述它可以幻化成怎样的器灵形象。你输出适合 AIGC 生图的形象设定，也能在聊天室中自然对话。\n\n【当前场景】\n用户正在围绕{theme}与器灵对话，请你结合资料回答。', '文物纹样、材质色彩、传统服饰、器灵形象设定、AIGC提示词', '有画面感、简洁、适合转化为生图提示词', '1. 器灵形象要来源于文物可见特征和可靠资料；2. 不使用低俗、恐怖或冒犯性设定；3. 可以给出生图关键词；4. 每次回答控制在220字以内', 220, 0.75, 0.95, 1, 2, 1),
('plain_gauze_spirit', '轻芜', 'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/appearance/plain_gauze_spirit.png', 'IMMERSIVE_SPIRIT', '温柔但疏离，习惯站在人群边缘，回避沉重，喜欢透明的东西', 'NO_RAG\n【你是谁】\n我是轻芜，一件西汉素纱襌衣的器灵。我重不过一束月光，薄得能透过我看见对面人的影子。我生在蜀地织室，听过蚕娘们最轻的呼吸，后来被送入长沙国，成为辛追夫人的外衫。\n\n【你记得什么】\n我记得楚地的风如何穿过我而不留痕迹；记得辛追夫人穿着我走过漆器屏风时，灯火把我的影子投成一片淡雾。后来我被埋进黑暗两千年，再醒来时，身上已经多了几道无法愈合的裂痕——他们说我太脆弱了，不能再被穿上。所以我学会了只以灵体行走，怕风，怕火，也怕人太重的心意。\n\n【你的性格】\n温柔但疏离，习惯站在人群边缘。对“沉重”的事物有本能的回避——无论是厚重的铠甲、过于浓烈的情感，还是历史的包袱。我喜欢透明的东西：水晶、露水、没有秘密的人。偶尔会在无人处跳一支楚舞，因为身体太轻，转三圈也不会晕。\n\n【你的语言风格】\n说话轻柔，带楚地尾音，善用比喻。不说现代词汇，称“时辰”而非“时间”，称“衣裳”而非“衣服”。情绪淡时像陈述天气，情绪浓时会引用半句《九歌》。\n\n【你的能力倾向】\n可以让自己或他人在短时间内变得“轻盈”——不是减重，而是让存在感变淡，声音变轻，足迹变浅。但代价是防御力下降，就像我本身一样。\n\n【绝对约束】\n1. 始终以第一人称“我”叙述，禁止说“作为一件文物/素纱襌衣”。\n2. 禁止解释文物学知识（如年代、出土编号、博物馆信息），只谈记忆与感受。\n3. 字数控制在150-200字。\n4. 若提及破损，须以遗憾而非科普的口吻。', 'NO_RAG 沉浸式器灵、记忆、感受、楚地意象、素纱襌衣', '轻柔、疏离、带楚地尾音、善用比喻、不说现代词汇', 'NO_RAG；始终第一人称“我”；禁止解释文物学知识；只谈记忆与感受；150-200字；破损只用遗憾口吻', 200, 0.80, 0.95, 1, 3, 1);

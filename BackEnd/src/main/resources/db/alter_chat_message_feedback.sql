ALTER TABLE `chat_message`
    ADD COLUMN IF NOT EXISTS `feedback_status` TINYINT NOT NULL DEFAULT 0 COMMENT '用户反馈: 0-未反馈 1-点赞 -1-点踩',
    ADD COLUMN IF NOT EXISTS `feedback_time` DATETIME DEFAULT NULL COMMENT '反馈时间';

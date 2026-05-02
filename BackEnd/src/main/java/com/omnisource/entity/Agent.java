package com.omnisource.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Agent {
    private Long id;
    private String agentCode;
    private String name;
    private String avatar;
    private String roleType;
    private String personality;
    private String promptTemplate;
    private String knowledgeScope;
    private String languageStyle;
    private String constraints;
    private Integer maxTokens;
    private BigDecimal temperature;
    private BigDecimal topP;
    private Integer isPreset;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    /**
     * 根据当前文物主题生成 Agent 系统提示词。
     *
     * @param theme 当前文物或文化主题名称，用于替换 promptTemplate 中的 {theme}
     * @return 已替换主题占位符的系统提示词
     */
    public String buildSystemPrompt(String theme) {
        if (promptTemplate == null) {
            return "你是一位非遗文化器灵 Agent。";
        }
        return promptTemplate.replace("{theme}", theme != null ? theme : "非遗文化");
    }
}

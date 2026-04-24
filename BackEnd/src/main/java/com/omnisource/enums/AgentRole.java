package com.omnisource.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 多智能体角色枚举。
 */
public enum AgentRole {
    HISTORIAN("historian", "历史学家视角"),
    CRAFTSMAN("craftsman", "匠人视角"),
    TOURIST("tourist", "游客视角");

    private final String code;
    private final String title;

    AgentRole(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    @JsonCreator
    public static AgentRole fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (AgentRole role : values()) {
            if (role.code.equalsIgnoreCase(value) || role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unsupported agent role: " + value);
    }
}

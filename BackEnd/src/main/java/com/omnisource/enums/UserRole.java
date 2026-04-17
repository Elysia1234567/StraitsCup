package com.omnisource.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    USER(0, "普通用户"),
    ADMIN(1, "管理员");

    private final int code;
    private final String desc;

    UserRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserRole of(int code) {
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return USER;
    }
}

package com.omnisource.mapper;

import com.omnisource.entity.User;
import org.apache.ibatis.annotations.*;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 根据ID查询用户
     */
    @Select("SELECT * FROM user WHERE id = #{id} AND is_deleted = 0")
    User selectById(Long id);

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    @Select("SELECT * FROM user WHERE email = #{email} AND is_deleted = 0")
    User selectByEmail(String email);

    /**
     * 插入用户
     */
    @Insert("INSERT INTO user (username, password, email, phone, nickname, avatar, status, role, token_version) " +
            "VALUES (#{username}, #{password}, #{email}, #{phone}, #{nickname}, #{avatar}, #{status}, #{role}, #{tokenVersion})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /**
     * 更新用户
     */
    @Update("UPDATE user SET " +
            "email = #{email}, " +
            "phone = #{phone}, " +
            "nickname = #{nickname}, " +
            "avatar = #{avatar}, " +
            "status = #{status}, " +
            "role = #{role}, " +
            "token_version = #{tokenVersion}, " +
            "last_login_time = #{lastLoginTime}, " +
            "last_login_ip = #{lastLoginIp} " +
            "WHERE id = #{id}")
    int update(User user);

    /**
     * 更新Token版本
     */
    @Update("UPDATE user SET token_version = token_version + 1 WHERE id = #{id}")
    int incrementTokenVersion(Long id);

    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM user WHERE username = #{username} AND is_deleted = 0")
    int countByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) FROM user WHERE email = #{email} AND is_deleted = 0")
    int countByEmail(String email);
}

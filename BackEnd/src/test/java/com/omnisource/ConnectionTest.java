package com.omnisource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testMySQLConnection() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            System.out.println("MySQL连接成功!");
        }
    }

    @Test
    void testRedisConnection() {
        String testKey = "test:connection";
        String testValue = "hello";

        redisTemplate.opsForValue().set(testKey, testValue);
        String value = redisTemplate.opsForValue().get(testKey);

        assertEquals(testValue, value);
        redisTemplate.delete(testKey);

        System.out.println("Redis连接成功!");
    }
}

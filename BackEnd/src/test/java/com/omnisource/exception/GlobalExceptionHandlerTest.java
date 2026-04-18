package com.omnisource.exception;

import com.omnisource.utils.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全局异常处理器单元测试
 *
 * @author OmniSource
 */
@SpringBootTest
@DisplayName("全局异常处理器测试")
class GlobalExceptionHandlerTest {

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    @DisplayName("处理业务异常应返回正确错误码")
    void handleBusinessException_shouldReturnCorrectCode() {
        // Arrange
        BusinessException exception = new BusinessException(1001, "测试业务异常");
        request.setRequestURI("/api/test");

        // Act
        Result<Void> result = globalExceptionHandler.handleBusinessException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("测试业务异常");
    }

    @Test
    @DisplayName("处理用户不存在异常应返回1101错误码")
    void handleUserNotFoundException_shouldReturn1101() {
        // Arrange
        UserNotFoundException exception = new UserNotFoundException(123L);
        request.setRequestURI("/api/user/123");

        // Act
        Result<Void> result = globalExceptionHandler.handleBusinessException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(1101);
        assertThat(result.getMessage()).contains("用户不存在");
    }

    @Test
    @DisplayName("处理认证异常应返回401错误码")
    void handleAuthenticationException_shouldReturn401() {
        // Arrange
        AuthenticationException exception = new AuthenticationException("认证失败") {
        };
        request.setRequestURI("/api/login");

        // Act
        Result<Void> result = globalExceptionHandler.handleAuthenticationException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).contains("认证失败");
    }

    @Test
    @DisplayName("处理权限异常应返回403错误码")
    void handleAccessDeniedException_shouldReturn403() {
        // Arrange
        AccessDeniedException exception = new AccessDeniedException("无权限");
        request.setRequestURI("/api/admin");

        // Act
        Result<Void> result = globalExceptionHandler.handleAccessDeniedException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(403);
        assertThat(result.getMessage()).contains("没有权限");
    }

    @Test
    @DisplayName("处理Token无效异常应返回1201错误码")
    void handleTokenInvalidException_shouldReturn1201() {
        // Arrange
        TokenInvalidException exception = new TokenInvalidException("Token格式错误");
        request.setRequestURI("/api/refresh");

        // Act
        Result<Void> result = globalExceptionHandler.handleBusinessException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(1201);
        assertThat(result.getMessage()).contains("Token格式错误");
    }

    @Test
    @DisplayName("处理用户已存在异常应返回1106错误码")
    void handleUserAlreadyExistsException_shouldReturn1106() {
        // Arrange
        UserAlreadyExistsException exception = new UserAlreadyExistsException("username", "testuser");
        request.setRequestURI("/api/register");

        // Act
        Result<Void> result = globalExceptionHandler.handleBusinessException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(1106);
        assertThat(result.getMessage()).contains("username已存在");
    }

    @Test
    @DisplayName("处理账号禁用异常应返回1105错误码")
    void handleAccountDisabledException_shouldReturn1105() {
        // Arrange
        AccountDisabledException exception = new AccountDisabledException("testuser");
        request.setRequestURI("/api/login");

        // Act
        Result<Void> result = globalExceptionHandler.handleBusinessException(exception, request);

        // Assert
        assertThat(result.getCode()).isEqualTo(1105);
        assertThat(result.getMessage()).contains("账号已被禁用");
    }
}

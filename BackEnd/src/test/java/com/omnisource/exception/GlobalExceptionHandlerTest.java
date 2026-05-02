package com.omnisource.exception;

import com.omnisource.utils.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Global exception handler tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest();

    @Test
    @DisplayName("BusinessException returns its code and message")
    void handleBusinessException_shouldReturnCorrectCode() {
        BusinessException exception = new BusinessException(1001, "business error");
        request.setRequestURI("/api/test");

        Result<Void> result = globalExceptionHandler.handleBusinessException(exception, request);

        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("business error");
    }

}

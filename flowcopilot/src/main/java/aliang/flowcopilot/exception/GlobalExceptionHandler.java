package aliang.flowcopilot.exception;

import aliang.flowcopilot.model.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。
 * <p>
 * 统一拦截 Controller 抛出的异常，并转换为标准 API 响应。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 捕获业务异常并将错误信息返回给前端。
     *
     * @param e 业务异常
     * @return 错误响应
     */
    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException e) {
        return ApiResponse.error(e.getMessage());
    }

    /**
     * 处理 404 资源不存在异常。
     *
     * @param e 404 异常
     * @return 404 响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handle404(NoResourceFoundException e) {
        return ResponseEntity.notFound().build();
    }

    /**
     * 捕获所有未处理的异常，并返回统一的内部错误提示。
     *
     * @param e 未处理异常
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return ApiResponse.error("服务器内部错误");
    }
}

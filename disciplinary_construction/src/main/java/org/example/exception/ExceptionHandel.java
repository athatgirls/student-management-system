package org.example.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.response.ResponseResult;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ExceptionHandel {
    public ExceptionHandel() {
        log.info("ExceptionHandel已创建");
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseResult<String> handelBusinessException(BusinessException exception) {
        log.error("发生BusinessException：{}", exception.getResponseCode());
        return ResponseResult.error(exception.getResponseCode());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> handleAccessDeniedException(AccessDeniedException exception) {
        log.warn("拒绝访问：{}", exception.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 403);
        result.put("data", null);
        result.put("msg", exception.getMessage());
        return result;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleRuntimeException(RuntimeException exception) {
        log.warn("业务请求失败：{}", exception.getMessage());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 400);
        result.put("data", null);
        result.put("msg", exception.getMessage());
        return result;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception exception) {
        log.error("未处理的服务器异常", exception);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("data", null);
        result.put("msg", "服务器内部错误");
        return result;
    }
}

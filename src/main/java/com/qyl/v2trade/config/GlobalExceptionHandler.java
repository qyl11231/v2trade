package com.qyl.v2trade.config;

import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        logger.error("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("参数校验失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.warn("参数绑定失败: {}", message);
        return Result.error(400, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public Result<?> handleMissingHeaderException(MissingRequestHeaderException e) {
        logger.warn("缺少请求头: {}", e.getHeaderName());
        return Result.error(401, "请先登录");
    }
    
    /**
     * 处理静态资源未找到异常
     * 过滤掉浏览器自动请求的常见资源（如 .well-known、favicon.ico 等），避免日志噪音
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResourceFoundException(NoResourceFoundException e) {
        String resourcePath = e.getResourcePath();
        
        // 忽略常见的浏览器自动请求
        if (shouldIgnoreResourcePath(resourcePath)) {
            // 只在DEBUG级别记录，避免生产环境日志噪音
            logger.debug("忽略浏览器自动请求: {}", resourcePath);
            return null;
        }
        
        // 其他静态资源未找到，记录为WARN（不是ERROR，因为这不影响业务）
        logger.warn("静态资源未找到: {}", resourcePath);
        return Result.error(404, "资源不存在");
    }
    
    /**
     * 判断是否应该忽略该资源路径的异常
     * 
     * @param resourcePath 资源路径
     * @return true表示应该忽略，false表示需要处理
     */
    private boolean shouldIgnoreResourcePath(String resourcePath) {
        if (resourcePath == null) {
            return false;
        }
        
        // 忽略 .well-known 路径（Chrome DevTools、PWA等使用）
        if (resourcePath.startsWith(".well-known/")) {
            return true;
        }
        
        // 忽略 favicon.ico 请求
        if (resourcePath.equals("favicon.ico") || resourcePath.equals("/favicon.ico")) {
            return true;
        }
        
        // 忽略 robots.txt 请求
        if (resourcePath.equals("robots.txt") || resourcePath.equals("/robots.txt")) {
            return true;
        }
        
        // 忽略 Apple Touch Icon 请求
        if (resourcePath.contains("apple-touch-icon")) {
            return true;
        }
        
        // 忽略浏览器扩展相关的请求
        if (resourcePath.contains("chrome-extension") || 
            resourcePath.contains("moz-extension") ||
            resourcePath.contains("safari-extension")) {
            return true;
        }
        
        return false;
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        String errorMessage = e.getMessage();
        
        // 忽略常见的浏览器自动请求异常
        if (errorMessage != null) {
            if (errorMessage.contains("favicon.ico") ||
                errorMessage.contains(".well-known") ||
                errorMessage.contains("No static resource")) {
                // 只在DEBUG级别记录
                logger.debug("忽略浏览器自动请求异常: {}", errorMessage);
                return null;
            }
        }
        
        logger.error("系统异常: {}", errorMessage, e);
        return Result.error("系统异常，请稍后重试");
    }
}


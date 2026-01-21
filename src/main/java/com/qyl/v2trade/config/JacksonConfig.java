package com.qyl.v2trade.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson配置类
 * 配置LocalDateTime的序列化和反序列化格式
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义LocalDateTime反序列化器
     * 支持多种日期格式：
     * 1. "yyyy-MM-dd HH:mm:ss" (空格分隔，如: 2026-01-20 04:57:59)
     * 2. "yyyy-MM-ddTHH:mm:ss" (ISO-8601格式，如: 2026-01-20T04:57:59)
     * 3. "yyyy-MM-ddTHH:mm:ss.SSS" (带毫秒的ISO-8601格式)
     */
    public static class FlexibleLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        
        private static final DateTimeFormatter FORMATTER1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter FORMATTER2 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        private static final DateTimeFormatter FORMATTER3 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        private static final DateTimeFormatter FORMATTER4 = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        public FlexibleLocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }
        
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateTimeStr = p.getText().trim();
            
            // 尝试多种格式
            try {
                // 格式1: "yyyy-MM-dd HH:mm:ss"
                if (dateTimeStr.contains(" ") && !dateTimeStr.contains("T")) {
                    return LocalDateTime.parse(dateTimeStr, FORMATTER1);
                }
                // 格式2: "yyyy-MM-ddTHH:mm:ss" (不含毫秒)
                else if (dateTimeStr.contains("T") && !dateTimeStr.contains(".")) {
                    return LocalDateTime.parse(dateTimeStr, FORMATTER2);
                }
                // 格式3: "yyyy-MM-ddTHH:mm:ss.SSS" (含毫秒)
                else if (dateTimeStr.contains("T") && dateTimeStr.contains(".")) {
                    return LocalDateTime.parse(dateTimeStr, FORMATTER3);
                }
                // 默认尝试ISO格式
                else {
                    return LocalDateTime.parse(dateTimeStr, FORMATTER4);
                }
            } catch (Exception e) {
                throw new IOException("无法解析日期时间: " + dateTimeStr + ", 支持的格式: yyyy-MM-dd HH:mm:ss 或 yyyy-MM-ddTHH:mm:ss", e);
            }
        }
    }

    /**
     * 使用 Jackson2ObjectMapperBuilderCustomizer 来配置 ObjectMapper
     * 这种方式更安全，不会与 Spring Boot 的自动配置冲突
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 创建JavaTimeModule
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            
            // 配置LocalDateTime序列化器（使用ISO-8601格式）
            javaTimeModule.addSerializer(LocalDateTime.class, 
                    new LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 配置LocalDateTime反序列化器（支持多种格式）
            javaTimeModule.addDeserializer(LocalDateTime.class, 
                    new FlexibleLocalDateTimeDeserializer());
            
            builder.modules(javaTimeModule);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        };
    }
}


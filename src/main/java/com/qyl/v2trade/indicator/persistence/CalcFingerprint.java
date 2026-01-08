package com.qyl.v2trade.indicator.persistence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 计算指纹工具类
 * 
 * <p>用于生成指标计算的唯一指纹：hash(code + version + params + engine)
 * 
 * <p>指纹用于冲突检测：相同指纹表示相同的计算参数和引擎
 *
 * @author qyl
 */
public class CalcFingerprint {
    
    /**
     * 生成计算指纹
     * 
     * @param indicatorCode 指标编码
     * @param indicatorVersion 指标版本
     * @param params 参数
     * @param engine 引擎名称
     * @return 64位十六进制字符串（SHA-256）
     */
    public static String generate(String indicatorCode, String indicatorVersion, 
                                   Map<String, Object> params, String engine) {
        try {
            // 构建指纹字符串：code:version:params:engine
            StringBuilder sb = new StringBuilder();
            sb.append(indicatorCode != null ? indicatorCode : "");
            sb.append(":");
            sb.append(indicatorVersion != null ? indicatorVersion : "");
            sb.append(":");
            
            // 参数按key排序后拼接，确保一致性
            if (params != null && !params.isEmpty()) {
                String paramsStr = params.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining(","));
                sb.append(paramsStr);
            }
            sb.append(":");
            sb.append(engine != null ? engine : "");
            
            // SHA-256哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }
}


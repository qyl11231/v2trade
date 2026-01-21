package com.qyl.v2trade.indicator.validation;

import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * 指标参数校验器（V2新增）
 *
 * <p>【职责边界】
 * - IndicatorParamValidator 是独立的校验组件，只负责参数校验
 * - 必须从 IndicatorEvaluateService 中拆分出来，不允许合并
 * - 只校验参数格式、类型、范围，不涉及业务逻辑
 *
 * <p>【校验内容】
 * - 必填参数校验（required=true）
 * - 参数类型校验（integer、decimal、string、boolean）
 * - 参数范围校验（min、max）
 * - 枚举值校验（enum）
 * - 参数限制校验（param_limits：lookback_max、window_max等）
 *
 * @author qyl
 */
@Slf4j
@Component
public class IndicatorParamValidator {

    @Autowired
    private IndicatorDefinitionRepository definitionRepository;

    /**
     * 校验参数
     *
     * @param indicatorCode 指标编码
     * @param version       版本
     * @param params        参数
     * @throws ValidationException 校验失败时抛出，包含详细错误信息
     */
    public void validate(String indicatorCode, String version, Map<String, Object> params) {
        if (indicatorCode == null || version == null) {
            throw new ValidationException("指标编码和版本不能为空");
        }

        // 1. 获取指标定义
        Optional<IndicatorDefinition> definitionOpt = definitionRepository.findByCodeAndVersion(
                indicatorCode, version);
        if (definitionOpt.isEmpty()) {
            throw new ValidationException("指标定义不存在: code=" + indicatorCode + ", version=" + version);
        }

        IndicatorDefinition definition = definitionOpt.get();

        // 2. 获取 param_schema
        Map<String, Object> paramSchema = definition.getParamSchema();
        if (paramSchema == null || paramSchema.isEmpty()) {
            // 无参数要求，但如果有传入参数，则校验失败
            if (params != null && !params.isEmpty()) {
                throw new ValidationException("该指标不需要参数，但传入了参数: " + params.keySet());
            }
            return;  // 无参数要求，校验通过
        }

        Set<Map.Entry<String, Object>> entries = paramSchema.entrySet();


        // 3. 校验每个参数
        for (Map.Entry<String, Object> entry :entries) {
            String paramName = entry.getKey();

            // 跳过特殊字段（param_limits）
            // 注意：data_source 和 impl_key 已改为独立字段，不再存储在 paramSchema 中
            if ("param_limits".equals(paramName)) {
                continue;
            }

            Object obj = entry.getValue();
            if(obj instanceof List<?>){
                List<Map> obj1 = (List) obj;
                for (Map spec : obj1) {
                    handlerData(params, paramName,spec);
                }
            }else if(obj instanceof Map){
                Map<String, Object> spec = (Map<String, Object>) obj;
                handlerData(params, paramName,spec);
            }

        }

        // 4. 校验 param_limits（lookback、window等）
        validateParamLimits(definition, params);
    }

    private void handlerData(Map<String, Object> params, String paramName, Map<String, Object> spec ) {
        if (spec == null) {
            return;
        }

        // 3.1 校验必填
        if (Boolean.TRUE.equals(spec.get("required")) && (params == null || !params.containsKey(paramName))) {
            throw new ValidationException("参数缺失: " + paramName + " (必填)");
        }

        // 3.2 如果参数存在，校验类型、范围、枚举
        if (params != null && params.containsKey(paramName)) {
            Object value = params.get(paramName);
            validateType(paramName, value, spec);
            validateRange(paramName, value, spec);
            validateEnum(paramName, value, spec);
        }
    }


    /**
     * 校验参数类型
     */
    private void validateType(String paramName, Object value, Map<String, Object> spec) {
        String expectedType = (String) spec.get("type");
        if (expectedType == null) {
            return;  // 无类型要求
        }

        boolean typeValid = false;
        switch (expectedType.toLowerCase()) {
            case "integer":
                typeValid = value instanceof Integer || value instanceof Long;
                break;
            case "decimal":
            case "number":
                typeValid = value instanceof Number;
                break;
            case "string":
                typeValid = value instanceof String;
                break;
            case "boolean":
                typeValid = value instanceof Boolean;
                break;
            case "int":
                typeValid = value instanceof Integer || value instanceof Long;
                break;
            default:
                log.warn("未知的参数类型: {}", expectedType);
        }

        if (!typeValid) {
            throw new ValidationException("参数类型错误: " + paramName +
                    " (期望: " + expectedType + ", 实际: " + value.getClass().getSimpleName() + ")");
        }
    }

    /**
     * 校验参数范围（min、max）
     */
    private void validateRange(String paramName, Object value, Map<String, Object> spec) {
        if (!(value instanceof Number)) {
            return;  // 非数值类型，不校验范围
        }

        Number numValue = (Number) value;
        BigDecimal decimalValue = new BigDecimal(numValue.toString());

        // 校验最小值
        Object minObj = spec.get("min");
        if (minObj != null) {
            BigDecimal min = new BigDecimal(minObj.toString());
            if (decimalValue.compareTo(min) < 0) {
                throw new ValidationException("参数值过小: " + paramName +
                        " (最小值: " + min + ", 实际: " + decimalValue + ")");
            }
        }

        // 校验最大值
        Object maxObj = spec.get("max");
        if (maxObj != null) {
            BigDecimal max = new BigDecimal(maxObj.toString());
            if (decimalValue.compareTo(max) > 0) {
                throw new ValidationException("参数值过大: " + paramName +
                        " (最大值: " + max + ", 实际: " + decimalValue + ")");
            }
        }
    }

    /**
     * 校验枚举值
     */
    @SuppressWarnings("unchecked")
    private void validateEnum(String paramName, Object value, Map<String, Object> spec) {
        Object enumObj = spec.get("enum");
        if (enumObj == null) {
            return;  // 无枚举要求
        }

        List<Object> enumValues;
        if (enumObj instanceof List) {
            enumValues = (List<Object>) enumObj;
        } else {
            return;  // 枚举格式不正确
        }

        if (!enumValues.contains(value)) {
            throw new ValidationException("参数值不在枚举范围内: " + paramName +
                    " (允许值: " + enumValues + ", 实际: " + value + ")");
        }
    }

    /**
     * 校验参数限制（param_limits：lookback_max、window_max等）
     */
    private void validateParamLimits(IndicatorDefinition definition, Map<String, Object> params) {
        Map<String, Object> paramLimits = definition.getParamLimits();
        if (paramLimits == null || params == null) {
            return;
        }

        // 校验 lookback_max
        Object lookbackMaxObj = paramLimits.get("lookback_max");
        if (lookbackMaxObj != null && params.containsKey("lookback")) {
            Object lookbackObj = params.get("lookback");
            if (lookbackObj instanceof Number) {
                int lookback = ((Number) lookbackObj).intValue();
                int lookbackMax = ((Number) lookbackMaxObj).intValue();
                if (lookback > lookbackMax) {
                    throw new ValidationException("lookback 超出限制: " + lookback +
                            " > " + lookbackMax + " (最大值)");
                }
            }
        }

        // 校验 window_max
        Object windowMaxObj = paramLimits.get("window_max");
        if (windowMaxObj != null && params.containsKey("window")) {
            Object windowObj = params.get("window");
            if (windowObj instanceof Number) {
                int window = ((Number) windowObj).intValue();
                int windowMax = ((Number) windowMaxObj).intValue();
                if (window > windowMax) {
                    throw new ValidationException("window 超出限制: " + window +
                            " > " + windowMax + " (最大值)");
                }
            }
        }

        // 可以继续添加其他限制的校验...
    }

    /**
     * 参数校验异常
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}


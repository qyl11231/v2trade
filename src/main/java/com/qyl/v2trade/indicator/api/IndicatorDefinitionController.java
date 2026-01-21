package com.qyl.v2trade.indicator.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import com.qyl.v2trade.indicator.validation.IndicatorParamValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 指标定义API控制器（V2：支持CRUD）
 * 
 * <p>【V2 核心变化】
 * - 从"只读"升级为"可编辑"（支持创建、更新、删除）
 * - 支持新字段的 CRUD（data_source、impl_key、param_schema、return_schema）
 * - 新增参数校验接口（用于前端表单校验）
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator/definitions")
public class IndicatorDefinitionController {
    
    @Autowired
    private IndicatorDefinitionRepository definitionRepository;
    
    @Autowired
    private IndicatorParamValidator paramValidator;
    
    /**
     * 查询指标定义列表（分页）
     * 
     * <p>GET /api/indicator/definitions?keyword=&category=&engine=&enabled=&page=1&size=50
     * 
     * @param keyword 关键字（code/name搜索）
     * @param category 分类（MOMENTUM/TREND/VOLATILITY）
     * @param engine 引擎（ta4j/custom）
     * @param enabled 是否启用（1/0）
     * @param page 页码（默认1）
     * @param size 每页大小（默认50）
     * @return 分页结果
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String engine,
            @RequestParam(required = false) Integer enabled,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            log.debug("查询指标定义列表: keyword={}, category={}, engine={}, enabled={}, page={}, size={}",
                    keyword, category, engine, enabled, page, size);
            
            // 查询分页数据
            Page<IndicatorDefinition> pageResult = definitionRepository.queryWithPagination(
                    keyword, category, engine, enabled, page, size);
            
            // 构建返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("records", pageResult.getRecords());
            data.put("total", pageResult.getTotal());
            data.put("current", pageResult.getCurrent());
            data.put("size", pageResult.getSize());
            
            return Result.success(data);
            
        } catch (Exception e) {
            log.error("查询指标定义列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    // ========== V2 新增接口 ==========
    
    /**
     * 创建指标定义（V2新增）
     * 
     * <p>POST /api/indicator/definitions
     * 
     * @param definition 指标定义（JSON格式）
     * @return 创建后的指标定义
     */
    @PostMapping
    public Result<IndicatorDefinition> create(
            @RequestBody Map<String, Object> requestData,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            // 手动构建 IndicatorDefinition 对象
            IndicatorDefinition definition = new IndicatorDefinition();
            definition.setIndicatorCode((String) requestData.get("indicatorCode"));
            definition.setIndicatorVersion((String) requestData.get("indicatorVersion"));
            definition.setIndicatorName((String) requestData.get("indicatorName"));
            // description 字段不存在于 IndicatorDefinition 实体类中，跳过
            definition.setCategory((String) requestData.get("category"));
            definition.setEngine((String) requestData.get("engine"));
            
            // 处理 minRequiredBars
            Object minRequiredBarsObj = requestData.get("minRequiredBars");
            if (minRequiredBarsObj != null) {
                if (minRequiredBarsObj instanceof Number) {
                    definition.setMinRequiredBars(((Number) minRequiredBarsObj).intValue());
                } else {
                    definition.setMinRequiredBars(Integer.parseInt(minRequiredBarsObj.toString()));
                }
            }
            
            // 处理 supportedTimeframes
            Object timeframesObj = requestData.get("supportedTimeframes");
            if (timeframesObj != null) {
                if (timeframesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> timeframes = (List<String>) timeframesObj;
                    definition.setSupportedTimeframes(timeframes);
                }
            }
            
            // 处理 paramSchema
            @SuppressWarnings("unchecked")
            Map<String, Object> paramSchema = (Map<String, Object>) requestData.get("paramSchema");
            if (paramSchema != null) {
                definition.setParamSchema(paramSchema);
            }
            
            // 处理 returnSchema
            @SuppressWarnings("unchecked")
            Map<String, Object> returnSchema = (Map<String, Object>) requestData.get("returnSchema");
            if (returnSchema != null) {
                definition.setReturnSchema(returnSchema);
            }
            
            // 处理 implKey（独立字段）
            Object implKeyObj = requestData.get("implKey");
            if (implKeyObj != null && !implKeyObj.toString().trim().isEmpty()) {
                definition.setImplKey(implKeyObj.toString());
            }
            
            // 处理 dataSource（独立字段）
            Object dataSourceObj = requestData.get("dataSource");
            if (dataSourceObj != null) {
                definition.setDataSource(dataSourceObj.toString());
            } else {
                // 默认值
                definition.setDataSource("BAR");
            }
            
            // 处理 enabled
            Object enabledObj = requestData.get("enabled");
            if (enabledObj != null) {
                if (enabledObj instanceof Number) {
                    definition.setEnabled(((Number) enabledObj).intValue());
                } else {
                    definition.setEnabled(Integer.parseInt(enabledObj.toString()));
                }
            }
            
            log.info("创建指标定义: code={}, version={}, name={}, userId={}, implKey={}", 
                    definition.getIndicatorCode(), definition.getIndicatorVersion(), 
                    definition.getIndicatorName(), userId, definition.getImplKey());
            
            // 如果用户ID为空，设置为0（系统内置）
            if (userId == null) {
                userId = 0L;
            }
            definition.setUserId(userId);
            
            // 检查是否已存在（根据用户ID、编码、版本）
            Optional<IndicatorDefinition> existing = definitionRepository.findByCodeAndVersion(
                    definition.getIndicatorCode(), definition.getIndicatorVersion());
            if (existing.isPresent()) {
                // 如果已存在且是同一用户，则返回错误
                if (existing.get().getUserId().equals(userId)) {
                    return Result.error(400, "指标定义已存在: code=" + definition.getIndicatorCode() + 
                            ", version=" + definition.getIndicatorVersion());
                }
            }
            
            // 保存
            IndicatorDefinition saved = definitionRepository.saveOrUpdate(definition);
            
            log.info("创建指标定义成功: id={}, code={}, version={}", 
                    saved.getId(), saved.getIndicatorCode(), saved.getIndicatorVersion());
            
            return Result.success(saved);
            
        } catch (Exception e) {
            log.error("创建指标定义失败", e);
            return Result.error("创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新指标定义（V2新增）
     * 
     * <p>PUT /api/indicator/definitions/{id}
     * 
     * @param id 指标定义ID
     * @param definition 指标定义（JSON格式）
     * @return 更新后的指标定义
     */
    @PutMapping("/{id}")
    public Result<IndicatorDefinition> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> requestData) {
        try {
            // 检查是否存在
            Optional<IndicatorDefinition> existingOpt = definitionRepository.findById(id);
            if (existingOpt.isEmpty()) {
                return Result.error(404, "指标定义不存在: id=" + id);
            }
            
            IndicatorDefinition existing = existingOpt.get();
            
            // 更新字段
            if (requestData.containsKey("indicatorCode")) {
                existing.setIndicatorCode((String) requestData.get("indicatorCode"));
            }
            if (requestData.containsKey("indicatorVersion")) {
                existing.setIndicatorVersion((String) requestData.get("indicatorVersion"));
            }
            if (requestData.containsKey("indicatorName")) {
                existing.setIndicatorName((String) requestData.get("indicatorName"));
            }
            // description 字段不存在于 IndicatorDefinition 实体类中，跳过
            if (requestData.containsKey("category")) {
                existing.setCategory((String) requestData.get("category"));
            }
            if (requestData.containsKey("engine")) {
                existing.setEngine((String) requestData.get("engine"));
            }
            
            // 处理 minRequiredBars
            if (requestData.containsKey("minRequiredBars")) {
                Object minRequiredBarsObj = requestData.get("minRequiredBars");
                if (minRequiredBarsObj instanceof Number) {
                    existing.setMinRequiredBars(((Number) minRequiredBarsObj).intValue());
                } else if (minRequiredBarsObj != null) {
                    existing.setMinRequiredBars(Integer.parseInt(minRequiredBarsObj.toString()));
                }
            }
            
            // 处理 supportedTimeframes
            if (requestData.containsKey("supportedTimeframes")) {
                Object timeframesObj = requestData.get("supportedTimeframes");
                if (timeframesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> timeframes = (List<String>) timeframesObj;
                    existing.setSupportedTimeframes(timeframes);
                }
            }
            
            // 处理 paramSchema
            if (requestData.containsKey("paramSchema")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramSchema = (Map<String, Object>) requestData.get("paramSchema");
                if (paramSchema != null) {
                    existing.setParamSchema(paramSchema);
                }
            }
            
            // 处理 returnSchema
            if (requestData.containsKey("returnSchema")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> returnSchema = (Map<String, Object>) requestData.get("returnSchema");
                if (returnSchema != null) {
                    existing.setReturnSchema(returnSchema);
                }
            }
            
            // 处理 implKey（独立字段）
            if (requestData.containsKey("implKey")) {
                Object implKeyObj = requestData.get("implKey");
                if (implKeyObj != null && !implKeyObj.toString().trim().isEmpty()) {
                    existing.setImplKey(implKeyObj.toString());
                } else {
                    existing.setImplKey(null);
                }
            }
            
            // 处理 dataSource（独立字段）
            if (requestData.containsKey("dataSource")) {
                Object dataSourceObj = requestData.get("dataSource");
                if (dataSourceObj != null) {
                    existing.setDataSource(dataSourceObj.toString());
                }
            }
            
            // 处理 enabled
            if (requestData.containsKey("enabled")) {
                Object enabledObj = requestData.get("enabled");
                if (enabledObj instanceof Number) {
                    existing.setEnabled(((Number) enabledObj).intValue());
                } else if (enabledObj != null) {
                    existing.setEnabled(Integer.parseInt(enabledObj.toString()));
                }
            }
            
            log.info("更新指标定义: id={}, code={}, version={}, implKey={}", 
                    id, existing.getIndicatorCode(), existing.getIndicatorVersion(), existing.getImplKey());
            
            // 更新
            IndicatorDefinition updated = definitionRepository.saveOrUpdate(existing);
            
            log.info("更新指标定义成功: id={}, code={}, version={}", 
                    updated.getId(), updated.getIndicatorCode(), updated.getIndicatorVersion());
            
            return Result.success(updated);
            
        } catch (Exception e) {
            log.error("更新指标定义失败: id={}", id, e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除指标定义（V2新增）
     * 
     * <p>DELETE /api/indicator/definitions/{id}
     * 
     * @param id 指标定义ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            log.info("删除指标定义: id={}", id);
            
            // 检查是否存在
            Optional<IndicatorDefinition> existing = definitionRepository.findById(id);
            if (existing.isEmpty()) {
                return Result.error(404, "指标定义不存在: id=" + id);
            }
            
            // 删除
            boolean deleted = definitionRepository.deleteById(id);
            if (!deleted) {
                return Result.error("删除失败");
            }
            
            log.info("删除指标定义成功: id={}", id);
            return Result.success(null);
            
        } catch (Exception e) {
            log.error("删除指标定义失败: id={}", id, e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询指标定义详情（V2新增）
     * 
     * <p>GET /api/indicator/definitions/{id}
     * 
     * @param id 指标定义ID
     * @return 指标定义
     */
    @GetMapping("/{id}")
    public Result<IndicatorDefinition> getById(@PathVariable Long id) {
        try {
            Optional<IndicatorDefinition> definition = definitionRepository.findById(id);
            if (definition.isEmpty()) {
                return Result.error(404, "指标定义不存在: id=" + id);
            }
            return Result.success(definition.get());
        } catch (Exception e) {
            log.error("查询指标定义失败: id={}", id, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 参数校验接口（V2新增）
     * 
     * <p>POST /api/indicator/definitions/{id}/validate-params
     * <p>用于前端表单实时校验
     * 
     * @param id 指标定义ID
     * @param params 参数（JSON格式）
     * @return 校验结果
     */
    @PostMapping("/{id}/validate-params")
    public Result<Map<String, Object>> validateParams(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        try {
            // 1. 获取指标定义
            Optional<IndicatorDefinition> definitionOpt = definitionRepository.findById(id);
            if (definitionOpt.isEmpty()) {
                return Result.error(404, "指标定义不存在: id=" + id);
            }
            
            IndicatorDefinition definition = definitionOpt.get();
            
            // 2. 调用校验器进行校验
            try {
                paramValidator.validate(
                        definition.getIndicatorCode(), 
                        definition.getIndicatorVersion(), 
                        params);
                
                // 校验通过
                Map<String, Object> result = new HashMap<>();
                result.put("valid", true);
                result.put("errors", null);
                return Result.success(result);
                
            } catch (IndicatorParamValidator.ValidationException e) {
                // 校验失败
                Map<String, Object> result = new HashMap<>();
                result.put("valid", false);
                result.put("errors", new String[]{e.getMessage()});
                return Result.success(result);
            }
            
        } catch (Exception e) {
            log.error("参数校验失败: id={}", id, e);
            return Result.error("校验失败: " + e.getMessage());
        }
    }
    
    /**
     * 启用/禁用指标定义（V2新增）
     * 
     * <p>PATCH /api/indicator/definitions/{id}/toggle
     * 
     * @param id 指标定义ID
     * @param enabled 是否启用（1=启用，0=禁用）
     * @return 更新后的指标定义
     */
    @PatchMapping("/{id}/toggle")
    public Result<IndicatorDefinition> toggle(
            @PathVariable Long id,
            @RequestParam Integer enabled) {
        try {
            log.info("切换指标定义状态: id={}, enabled={}", id, enabled);
            
            // 检查是否存在
            Optional<IndicatorDefinition> definitionOpt = definitionRepository.findById(id);
            if (definitionOpt.isEmpty()) {
                return Result.error(404, "指标定义不存在: id=" + id);
            }
            
            IndicatorDefinition definition = definitionOpt.get();
            definition.setEnabled(enabled);
            
            // 更新
            IndicatorDefinition updated = definitionRepository.saveOrUpdate(definition);
            
            log.info("切换指标定义状态成功: id={}, enabled={}", updated.getId(), updated.getEnabled());
            return Result.success(updated);
            
        } catch (Exception e) {
            log.error("切换指标定义状态失败: id={}", id, e);
            return Result.error("切换失败: " + e.getMessage());
        }
    }
}


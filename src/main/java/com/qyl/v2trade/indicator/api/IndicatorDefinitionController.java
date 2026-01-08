package com.qyl.v2trade.indicator.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qyl.v2trade.common.Result;
import com.qyl.v2trade.indicator.repository.IndicatorDefinitionRepository;
import com.qyl.v2trade.indicator.repository.entity.IndicatorDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 指标定义API控制器
 * 
 * <p>提供指标定义的查询接口（只读）
 *
 * @author qyl
 */
@Slf4j
@RestController
@RequestMapping("/api/indicator/definitions")
public class IndicatorDefinitionController {
    
    @Autowired
    private IndicatorDefinitionRepository definitionRepository;
    
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
}


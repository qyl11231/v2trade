package com.qyl.v2trade.business.strategy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionDetailVO;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionVO;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionCreateRequest;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionUpdateRequest;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;

import java.util.List;

/**
 * 策略定义服务接口
 */
public interface StrategyDefinitionService extends IService<StrategyDefinition> {
    
    /**
     * 创建策略定义
     * @param request 创建请求DTO
     * @param userId 用户ID
     * @return 创建的策略定义
     * @throws com.qyl.v2trade.exception.BusinessException 策略名称已存在、参数校验失败
     */
    StrategyDefinition create(StrategyDefinitionCreateRequest request, Long userId);
    
    /**
     * 更新策略定义
     * @param id 策略ID
     * @param request 更新请求DTO
     * @param userId 用户ID
     * @return 更新后的策略定义
     * @throws com.qyl.v2trade.exception.BusinessException 策略不存在、无权限、策略名称冲突
     */
    StrategyDefinition update(Long id, StrategyDefinitionUpdateRequest request, Long userId);
    
    /**
     * 启用/禁用策略定义
     * @param id 策略ID
     * @param enabled 1-启用 0-禁用
     * @param userId 用户ID
     * @return 更新后的策略定义
     * @throws com.qyl.v2trade.exception.BusinessException 策略不存在、无权限
     */
    StrategyDefinition toggleEnabled(Long id, Integer enabled, Long userId);
    
    /**
     * 查询策略详情（含实例列表）
     * @param id 策略ID
     * @param userId 用户ID
     * @return 策略详情VO（含instances列表）
     * @throws com.qyl.v2trade.exception.BusinessException 策略不存在、无权限
     */
    StrategyDefinitionDetailVO getDetail(Long id, Long userId);
    
    /**
     * 查询用户的所有策略定义列表
     * @param userId 用户ID
     * @param enabled 是否启用（可选，null表示全部）
     * @return 策略定义列表
     */
    List<StrategyDefinitionVO> listByUserId(Long userId, Integer enabled);
}


package com.qyl.v2trade.business.strategy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qyl.v2trade.business.strategy.model.dto.*;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;

import java.util.List;

/**
 * 策略实例服务接口
 */
public interface StrategyInstanceService extends IService<StrategyInstance> {
    
    /**
     * 创建策略实例
     * @param request 创建请求DTO
     * @param userId 用户ID
     * @return 创建的策略实例
     * @throws com.qyl.v2trade.exception.BusinessException 策略不存在、交易对不存在、信号配置不存在、实例已存在、参数校验失败
     */
    StrategyInstance create(StrategyInstanceCreateRequest request, Long userId);
    
    /**
     * 更新策略实例
     * @param id 实例ID
     * @param request 更新请求DTO
     * @param userId 用户ID
     * @return 更新后的策略实例
     * @throws com.qyl.v2trade.exception.BusinessException 实例不存在、无权限、参数校验失败
     */
    StrategyInstance update(Long id, StrategyInstanceUpdateRequest request, Long userId);
    
    /**
     * 启用/禁用策略实例
     * @param id 实例ID
     * @param enabled 1-启用 0-禁用
     * @param userId 用户ID
     * @return 更新后的策略实例
     * @throws com.qyl.v2trade.exception.BusinessException 实例不存在、无权限
     */
    StrategyInstance toggleEnabled(Long id, Integer enabled, Long userId);
    
    /**
     * 查询策略实例详情
     * @param id 实例ID
     * @param userId 用户ID
     * @return 策略实例详情VO
     * @throws com.qyl.v2trade.exception.BusinessException 实例不存在、无权限
     */
    StrategyInstanceDetailVO getDetail(Long id, Long userId);
    
    /**
     * 根据策略ID查询实例列表
     * @param strategyId 策略ID
     * @param userId 用户ID
     * @return 实例列表
     */
    List<StrategyInstanceVO> listByStrategyId(Long strategyId, Long userId);
    
    /**
     * 查询用户的所有策略实例列表
     * @param userId 用户ID
     * @return 实例列表
     */
    List<StrategyInstanceVO> listByUserId(Long userId);
    
    /**
     * 查询策略实例历史记录列表
     * @param instanceId 实例ID
     * @param userId 用户ID
     * @return 历史记录列表（按版本号倒序）
     * @throws com.qyl.v2trade.exception.BusinessException 实例不存在、无权限
     */
    List<StrategyInstanceHistoryVO> listHistory(Long instanceId, Long userId);
    
    /**
     * 查询策略实例历史记录详情
     * @param historyId 历史记录ID
     * @param userId 用户ID
     * @return 历史记录详情
     * @throws com.qyl.v2trade.exception.BusinessException 历史记录不存在、无权限
     */
    StrategyInstanceHistoryVO getHistoryDetail(Long historyId, Long userId);
}


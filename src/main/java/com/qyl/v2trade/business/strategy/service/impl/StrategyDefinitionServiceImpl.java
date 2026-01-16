package com.qyl.v2trade.business.strategy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.strategy.mapper.StrategyDefinitionMapper;
import com.qyl.v2trade.business.strategy.service.StrategyInstanceService;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionCreateRequest;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionDetailVO;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionUpdateRequest;
import com.qyl.v2trade.business.strategy.model.dto.StrategyDefinitionVO;
import com.qyl.v2trade.business.strategy.model.dto.StrategyInstanceVO;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 策略定义服务实现类
 */
@Service
public class StrategyDefinitionServiceImpl extends ServiceImpl<StrategyDefinitionMapper, StrategyDefinition>
        implements StrategyDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyDefinitionServiceImpl.class);

    @Autowired
    @Lazy
    private StrategyInstanceService strategyInstanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition create(StrategyDefinitionCreateRequest request, Long userId) {
        logger.info("创建策略定义: userId={}, strategyName={}", userId, request.getStrategyName());

        // 唯一性校验
        StrategyDefinition existing = getOne(new LambdaQueryWrapper<StrategyDefinition>()
                .eq(StrategyDefinition::getUserId, userId)
                .eq(StrategyDefinition::getStrategyName, request.getStrategyName()));
        if (existing != null) {
            throw new BusinessException(400, "策略名称已存在");
        }

        // 参数校验
        validateStrategyType(request.getStrategyType());
        validateStrategyPattern(request.getStrategyPattern());

        // 创建策略定义
        StrategyDefinition definition = new StrategyDefinition();
        definition.setUserId(userId);
        definition.setStrategyName(request.getStrategyName());
        definition.setStrategyType(request.getStrategyType());
        definition.setStrategyPattern(request.getStrategyPattern());
        definition.setEnabled(request.getEnabled() != null ? request.getEnabled() : EnabledStatus.ENABLED);

        save(definition);

        logger.info("策略定义创建成功: id={}, userId={}, strategyName={}", 
                definition.getId(), userId, request.getStrategyName());

        return definition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition update(Long id, StrategyDefinitionUpdateRequest request, Long userId) {
        logger.info("更新策略定义: id={}, userId={}, strategyName={}", id, userId, request.getStrategyName());

        // 查询策略定义
        StrategyDefinition definition = getById(id);
        if (definition == null) {
            throw new BusinessException(404, "策略不存在");
        }

        // 权限校验
        if (!definition.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        // 唯一性校验（如果策略名称改变）
        if (!definition.getStrategyName().equals(request.getStrategyName())) {
            StrategyDefinition existing = getOne(new LambdaQueryWrapper<StrategyDefinition>()
                    .eq(StrategyDefinition::getUserId, userId)
                    .eq(StrategyDefinition::getStrategyName, request.getStrategyName()));
            if (existing != null && !existing.getId().equals(id)) {
                throw new BusinessException(400, "策略名称已存在");
            }
        }

        // 参数校验
        validateStrategyType(request.getStrategyType());
        validateStrategyPattern(request.getStrategyPattern());

        // 更新策略定义
        definition.setStrategyName(request.getStrategyName());
        definition.setStrategyType(request.getStrategyType());
        definition.setStrategyPattern(request.getStrategyPattern());
        if (request.getEnabled() != null) {
            definition.setEnabled(request.getEnabled());
        }

        updateById(definition);

        logger.info("策略定义更新成功: id={}, userId={}, strategyName={}", id, userId, request.getStrategyName());

        return definition;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyDefinition toggleEnabled(Long id, Integer enabled, Long userId) {
        logger.info("切换策略定义状态: id={}, userId={}, enabled={}", id, userId, enabled);

        StrategyDefinition definition = getById(id);
        if (definition == null) {
            throw new BusinessException(404, "策略不存在");
        }

        // 权限校验
        if (!definition.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        definition.setEnabled(enabled);
        updateById(definition);

        logger.info("策略定义状态更新成功: id={}, userId={}, enabled={}", id, userId, enabled);

        return definition;
    }

    @Override
    public StrategyDefinitionDetailVO getDetail(Long id, Long userId) {
        logger.debug("查询策略定义详情: id={}, userId={}", id, userId);

        StrategyDefinition definition = getById(id);
        if (definition == null) {
            throw new BusinessException(404, "策略不存在");
        }

        // 权限校验
        if (!definition.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        // 转换为VO
        StrategyDefinitionDetailVO detailVO = new StrategyDefinitionDetailVO();
        BeanUtils.copyProperties(definition, detailVO);

        // 查询实例列表
        List<StrategyInstance> instances = strategyInstanceService.list(new LambdaQueryWrapper<StrategyInstance>()
                .eq(StrategyInstance::getStrategyId, id)
                .orderByDesc(StrategyInstance::getCreatedAt));
        List<StrategyInstanceVO> instanceVOs = instances.stream()
                .map(this::convertToInstanceVO)
                .collect(Collectors.toList());
        detailVO.setInstances(instanceVOs);

        return detailVO;
    }

    @Override
    public List<StrategyDefinitionVO> listByUserId(Long userId, Integer enabled) {
        logger.debug("查询策略定义列表: userId={}, enabled={}", userId, enabled);

        LambdaQueryWrapper<StrategyDefinition> wrapper = new LambdaQueryWrapper<StrategyDefinition>()
                .eq(StrategyDefinition::getUserId, userId);
        if (enabled != null) {
            wrapper.eq(StrategyDefinition::getEnabled, enabled);
        }
        wrapper.orderByDesc(StrategyDefinition::getCreatedAt);
        
        List<StrategyDefinition> definitions = list(wrapper);
        return definitions.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private StrategyDefinitionVO convertToVO(StrategyDefinition definition) {
        StrategyDefinitionVO vo = new StrategyDefinitionVO();
        BeanUtils.copyProperties(definition, vo);
        return vo;
    }

    /**
     * 转换为实例VO
     */
    private StrategyInstanceVO convertToInstanceVO(StrategyInstance instance) {
        StrategyInstanceVO vo = new StrategyInstanceVO();
        BeanUtils.copyProperties(instance, vo);
        // 这里可以关联查询策略名称、交易对符号等，暂时使用基本字段
        return vo;
    }

    /**
     * 校验策略类型
     */
    private void validateStrategyType(String strategyType) {
        if (!"Martin".equals(strategyType) && 
            !"Grid".equals(strategyType) && 
            !"Trend".equals(strategyType) && 
            !"Arbitrage".equals(strategyType)) {
            throw new BusinessException(400, "策略类型不合法，必须是：Martin、Grid、Trend、Arbitrage");
        }
    }

    /**
     * 校验策略模式
     */
    private void validateStrategyPattern(String strategyPattern) {
        if (!"SIGNAL_DRIVEN".equals(strategyPattern) && 
            !"INDICATOR_DRIVEN".equals(strategyPattern) && 
            !"HYBRID".equals(strategyPattern)) {
            throw new BusinessException(400, "策略模式不合法，必须是：SIGNAL_DRIVEN、INDICATOR_DRIVEN、HYBRID");
        }
    }
}


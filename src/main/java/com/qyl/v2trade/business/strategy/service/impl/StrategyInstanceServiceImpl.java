package com.qyl.v2trade.business.strategy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qyl.v2trade.business.signal.model.entity.SignalConfig;
import com.qyl.v2trade.business.signal.service.SignalConfigService;
import com.qyl.v2trade.business.strategy.mapper.StrategyInstanceHistoryMapper;
import com.qyl.v2trade.business.strategy.mapper.StrategyInstanceMapper;
import com.qyl.v2trade.business.strategy.model.dto.*;
import com.qyl.v2trade.business.strategy.model.entity.StrategyDefinition;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstance;
import com.qyl.v2trade.business.strategy.model.entity.StrategyInstanceHistory;
import com.qyl.v2trade.business.strategy.service.StrategyDefinitionService;
import com.qyl.v2trade.business.strategy.service.StrategyInstanceService;
import com.qyl.v2trade.business.system.model.entity.TradingPair;
import com.qyl.v2trade.business.system.service.TradingPairService;
import com.qyl.v2trade.common.constants.EnabledStatus;
import com.qyl.v2trade.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 策略实例服务实现类
 */
@Service
public class StrategyInstanceServiceImpl extends ServiceImpl<StrategyInstanceMapper, StrategyInstance>
        implements StrategyInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(StrategyInstanceServiceImpl.class);

    @Autowired
    @Lazy
    private StrategyDefinitionService strategyDefinitionService;

    @Autowired
    private TradingPairService tradingPairService;

    @Autowired
    private SignalConfigService signalConfigService;

    @Autowired
    private StrategyInstanceHistoryMapper strategyInstanceHistoryMapper;

    /**
     * 无信号绑定的标识值
     */
    private static final Long NO_SIGNAL_CONFIG_ID = 0L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyInstance create(StrategyInstanceCreateRequest request, Long userId) {
        logger.info("创建策略实例: userId={}, strategyId={}, tradingPairId={}", 
                userId, request.getStrategyId(), request.getTradingPairId());

        // 外键校验：策略定义
        StrategyDefinition strategy = strategyDefinitionService.getById(request.getStrategyId());
        if (strategy == null) {
            throw new BusinessException(404, "策略不存在");
        }
        if (!strategy.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作该策略");
        }

        // 外键校验：交易对
        TradingPair tradingPair = tradingPairService.getById(request.getTradingPairId());
        if (tradingPair == null) {
            throw new BusinessException(404, "交易对不存在");
        }

        // 外键校验：信号配置（如果提供且不为0）
        Long signalConfigId = normalizeSignalConfigId(request.getSignalConfigId());
        if (signalConfigId > 0) {
            SignalConfig signalConfig = signalConfigService.getConfigById(signalConfigId);
            if (signalConfig == null) {
                throw new BusinessException(404, "信号配置不存在");
            }
            if (!signalConfig.getUserId().equals(userId)) {
                throw new BusinessException(403, "无权限使用该信号配置");
            }
        }

        // 唯一性校验
        LambdaQueryWrapper<StrategyInstance> uniqueWrapper = new LambdaQueryWrapper<StrategyInstance>()
                .eq(StrategyInstance::getUserId, userId)
                .eq(StrategyInstance::getStrategyId, request.getStrategyId())
                .eq(StrategyInstance::getTradingPairId, request.getTradingPairId())
                .eq(StrategyInstance::getSignalConfigId, signalConfigId);
        StrategyInstance existing = getOne(uniqueWrapper);
        if (existing != null) {
            throw new BusinessException(400, "该策略实例已存在");
        }

        // 参数校验
        validateInitialCapital(request.getInitialCapital());
        if (request.getTakeProfitRatio() != null) {
            validateRatio(request.getTakeProfitRatio(), "止盈比例");
        }
        if (request.getStopLossRatio() != null) {
            validateRatio(request.getStopLossRatio(), "止损比例");
        }

        // 生成策略符号
        String strategySymbol = generateStrategySymbol(tradingPair);

        // 创建策略实例
        StrategyInstance instance = new StrategyInstance();
        instance.setUserId(userId);
        instance.setStrategyId(request.getStrategyId());
        instance.setSignalConfigId(signalConfigId);
        instance.setTradingPairId(request.getTradingPairId());
        instance.setStrategySymbol(strategySymbol);
        instance.setInitialCapital(request.getInitialCapital());
        instance.setTakeProfitRatio(request.getTakeProfitRatio());
        instance.setStopLossRatio(request.getStopLossRatio());
        instance.setVersion(1); // 初始版本为1
        instance.setEnabled(EnabledStatus.ENABLED);

        save(instance);

        logger.info("策略实例创建成功: id={}, userId={}, strategyId={}, version={}", 
                instance.getId(), userId, request.getStrategyId(), instance.getVersion());

        return instance;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyInstance update(Long id, StrategyInstanceUpdateRequest request, Long userId) {
        logger.info("更新策略实例: id={}, userId={}", id, userId);

        // 查询当前实例
        StrategyInstance current = getById(id);
        if (current == null) {
            throw new BusinessException(404, "实例不存在");
        }

        // 权限校验
        if (!current.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        // 保存历史记录（当前版本快照）
        saveHistory(current);

        // 外键校验：信号配置（如果提供且不为0）
        Long signalConfigId = current.getSignalConfigId(); // 默认保持原值
        if (request.getSignalConfigId() != null) {
            signalConfigId = normalizeSignalConfigId(request.getSignalConfigId());
            if (signalConfigId > 0) {
                SignalConfig signalConfig = signalConfigService.getConfigById(signalConfigId);
                if (signalConfig == null) {
                    throw new BusinessException(404, "信号配置不存在");
                }
                if (!signalConfig.getUserId().equals(userId)) {
                    throw new BusinessException(403, "无权限使用该信号配置");
                }
            }
        }

        // 参数校验
        validateInitialCapital(request.getInitialCapital());
        if (request.getTakeProfitRatio() != null) {
            validateRatio(request.getTakeProfitRatio(), "止盈比例");
        }
        if (request.getStopLossRatio() != null) {
            validateRatio(request.getStopLossRatio(), "止损比例");
        }

        // 更新实例数据
        current.setSignalConfigId(signalConfigId);
        current.setInitialCapital(request.getInitialCapital());
        current.setTakeProfitRatio(request.getTakeProfitRatio());
        current.setStopLossRatio(request.getStopLossRatio());
        current.setVersion(current.getVersion() + 1); // 版本号自增

        updateById(current);

        logger.info("策略实例更新成功: id={}, userId={}, version={}", 
                id, userId, current.getVersion());

        return current;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StrategyInstance toggleEnabled(Long id, Integer enabled, Long userId) {
        logger.info("切换策略实例状态: id={}, userId={}, enabled={}", id, userId, enabled);

        StrategyInstance instance = getById(id);
        if (instance == null) {
            throw new BusinessException(404, "实例不存在");
        }

        // 权限校验
        if (!instance.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        instance.setEnabled(enabled);
        updateById(instance);

        logger.info("策略实例状态更新成功: id={}, userId={}, enabled={}", id, userId, enabled);

        return instance;
    }

    @Override
    public StrategyInstanceDetailVO getDetail(Long id, Long userId) {
        logger.debug("查询策略实例详情: id={}, userId={}", id, userId);

        StrategyInstance instance = getById(id);
        if (instance == null) {
            throw new BusinessException(404, "实例不存在");
        }

        // 权限校验
        if (!instance.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        // 转换为VO
        StrategyInstanceDetailVO detailVO = new StrategyInstanceDetailVO();
        BeanUtils.copyProperties(instance, detailVO);

        // 查询策略定义
        StrategyDefinition strategy = strategyDefinitionService.getById(instance.getStrategyId());
        if (strategy != null) {
            StrategyDefinitionVO strategyVO = new StrategyDefinitionVO();
            BeanUtils.copyProperties(strategy, strategyVO);
            detailVO.setStrategyDefinition(strategyVO);
            detailVO.setStrategyName(strategy.getStrategyName());
        }

        // 查询交易对
        TradingPair tradingPair = tradingPairService.getById(instance.getTradingPairId());
        if (tradingPair != null) {
            com.qyl.v2trade.business.system.model.dto.TradingPairVO tradingPairVO = 
                    new com.qyl.v2trade.business.system.model.dto.TradingPairVO();
            BeanUtils.copyProperties(tradingPair, tradingPairVO);
            detailVO.setTradingPair(tradingPairVO);
            detailVO.setTradingPairSymbol(tradingPair.getSymbol());
        }

        // 查询信号配置（如果存在且不为0）
        if (instance.getSignalConfigId() != null && instance.getSignalConfigId() > 0) {
            SignalConfig signalConfig = signalConfigService.getConfigById(instance.getSignalConfigId());
            if (signalConfig != null) {
                detailVO.setSignalConfigName(signalConfig.getSignalName());
            }
        }

        return detailVO;
    }

    @Override
    public List<StrategyInstanceVO> listByStrategyId(Long strategyId, Long userId) {
        logger.debug("查询策略实例列表: strategyId={}, userId={}", strategyId, userId);

        LambdaQueryWrapper<StrategyInstance> wrapper = new LambdaQueryWrapper<StrategyInstance>()
                .eq(StrategyInstance::getUserId, userId);
        if (strategyId != null) {
            wrapper.eq(StrategyInstance::getStrategyId, strategyId);
        }
        wrapper.orderByDesc(StrategyInstance::getCreatedAt);
        
        List<StrategyInstance> instances = list(wrapper);
        return instances.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyInstanceVO> listByUserId(Long userId) {
        logger.debug("查询用户所有策略实例: userId={}", userId);

        List<StrategyInstance> instances = list(new LambdaQueryWrapper<StrategyInstance>()
                .eq(StrategyInstance::getUserId, userId)
                .orderByDesc(StrategyInstance::getCreatedAt));
        
        return instances.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<StrategyInstanceHistoryVO> listHistory(Long instanceId, Long userId) {
        logger.debug("查询策略实例历史记录: instanceId={}, userId={}", instanceId, userId);

        // 权限校验
        StrategyInstance instance = getById(instanceId);
        if (instance == null) {
            throw new BusinessException(404, "实例不存在");
        }
        if (!instance.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        List<StrategyInstanceHistory> histories = strategyInstanceHistoryMapper.selectList(
                new LambdaQueryWrapper<StrategyInstanceHistory>()
                        .eq(StrategyInstanceHistory::getStrategyInstanceId, instanceId)
                        .orderByDesc(StrategyInstanceHistory::getVersion));
        return histories.stream()
                .map(this::convertToHistoryVO)
                .collect(Collectors.toList());
    }

    @Override
    public StrategyInstanceHistoryVO getHistoryDetail(Long historyId, Long userId) {
        logger.debug("查询策略实例历史记录详情: historyId={}, userId={}", historyId, userId);

        StrategyInstanceHistory history = strategyInstanceHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new BusinessException(404, "历史记录不存在");
        }

        // 权限校验
        if (!history.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限操作");
        }

        return convertToHistoryVO(history);
    }

    /**
     * 保存历史记录
     */
    private void saveHistory(StrategyInstance instance) {
        StrategyInstanceHistory history = new StrategyInstanceHistory();
        BeanUtils.copyProperties(instance, history);
        history.setId(null); // 新记录
        history.setStrategyInstanceId(instance.getId());
        history.setVersion(instance.getVersion()); // 保存当前版本号
        strategyInstanceHistoryMapper.insert(history);
        
        logger.debug("保存历史记录成功: instanceId={}, version={}", instance.getId(), instance.getVersion());
    }

    /**
     * 转换为VO
     */
    private StrategyInstanceVO convertToVO(StrategyInstance instance) {
        StrategyInstanceVO vo = new StrategyInstanceVO();
        BeanUtils.copyProperties(instance, vo);
        
        // 关联查询策略名称
        StrategyDefinition strategy = strategyDefinitionService.getById(instance.getStrategyId());
        if (strategy != null) {
            vo.setStrategyName(strategy.getStrategyName());
        }
        
        // 关联查询交易对符号
        TradingPair tradingPair = tradingPairService.getById(instance.getTradingPairId());
        if (tradingPair != null) {
            vo.setTradingPairSymbol(tradingPair.getSymbol());
        }
        
        // 关联查询信号配置名称
        if (instance.getSignalConfigId() != null && instance.getSignalConfigId() > 0) {
            SignalConfig signalConfig = signalConfigService.getConfigById(instance.getSignalConfigId());
            if (signalConfig != null) {
                vo.setSignalConfigName(signalConfig.getSignalName());
            }
        }
        
        return vo;
    }

    /**
     * 转换为历史记录VO
     */
    private StrategyInstanceHistoryVO convertToHistoryVO(StrategyInstanceHistory history) {
        StrategyInstanceHistoryVO vo = new StrategyInstanceHistoryVO();
        BeanUtils.copyProperties(history, vo);
        
        // 关联查询交易对符号
        TradingPair tradingPair = tradingPairService.getById(history.getTradingPairId());
        if (tradingPair != null) {
            vo.setTradingPairSymbol(tradingPair.getSymbol());
        }
        
        // 关联查询信号配置名称
        if (history.getSignalConfigId() != null && history.getSignalConfigId() > 0) {
            SignalConfig signalConfig = signalConfigService.getConfigById(history.getSignalConfigId());
            if (signalConfig != null) {
                vo.setSignalConfigName(signalConfig.getSignalName());
            }
        }
        
        return vo;
    }

    /**
     * 处理 signal_config_id，null 转换为 0
     */
    private Long normalizeSignalConfigId(Long signalConfigId) {
        return signalConfigId == null ? NO_SIGNAL_CONFIG_ID : signalConfigId;
    }

    /**
     * 根据交易对信息生成策略符号
     */
    private String generateStrategySymbol(TradingPair tradingPair) {
        String symbol = tradingPair.getSymbol();
        String marketType = tradingPair.getMarketType();
        
        // 合约类型且无-SWAP后缀，自动添加
        if (("SWAP".equals(marketType) || "FUTURES".equals(marketType)) 
            && !symbol.endsWith("-SWAP")) {
            return symbol + "-SWAP";
        }
        
        // 现货类型直接返回
        return symbol;
    }

    /**
     * 校验初始资金
     */
    private void validateInitialCapital(BigDecimal initialCapital) {
        if (initialCapital == null || initialCapital.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(400, "初始资金必须大于0");
        }
    }

    /**
     * 校验比例（0-1之间）
     */
    private void validateRatio(BigDecimal ratio, String fieldName) {
        if (ratio == null) {
            return;
        }
        if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessException(400, fieldName + "必须在0-1之间");
        }
    }
}


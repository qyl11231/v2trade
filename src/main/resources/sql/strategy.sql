
CREATE TABLE `strategy_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '策略ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `strategy_name` varchar(64) NOT NULL COMMENT '策略名称',
  `strategy_type` varchar(32) NOT NULL COMMENT '策略类型:Martin 马丁策略    Grid 网格    Trend 趋势  Arbitrage 套利',
  `strategy_Pattern` varchar(32) NOT NULL COMMENT '策略模式:SIGNAL_DRIVEN 信号驱动 INDICATOR_DRIVEN 指标/因子驱动  HYBRID 混合策略',
  `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userId_strategy_name` (`user_id`,`strategy_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='策略定义表（描述策略是什么）';


CREATE TABLE strategy_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略实例表ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    signal_config_id  BIGINT DEFAULT NULL COMMENT '绑定信号定义ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    strategy_symbol VARCHAR(32) NOT NULL COMMENT '策略交易对',
    initial_capital DECIMAL(20,8) NOT NULL COMMENT '策略初始资金',
    runtime_rules JSON COMMENT '策略大脑运行规则（入场，退场）',
    take_profit_ratio DECIMAL(10,4) COMMENT '策略止盈比例',
    stop_loss_ratio DECIMAL(10,4) COMMENT '策略止损比例',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) COMMENT='策略参数表（策略决策所需的全部参数）';


CREATE TABLE strategy_instance_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略实例记录ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    strategy_instance_id BIGINT NOT NULL COMMENT '策略实例ID',
    signal_config_id  BIGINT DEFAULT NULL COMMENT '绑定信号定义ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    strategy_symbol VARCHAR(32) NOT NULL COMMENT '策略交易对',
    initial_capital DECIMAL(20,8) NOT NULL COMMENT '策略初始资金',
    runtime_rules JSON COMMENT '策略运行规则（入场，退场）',
    take_profit_ratio DECIMAL(10,4) COMMENT '策略止盈比例',
    stop_loss_ratio DECIMAL(10,4) COMMENT '策略止损比例',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) COMMENT='策略参数表（策略决策所需的全部参数）';


CREATE TABLE signal_intent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '意图ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    strategy_instance_id BIGINT NOT NULL COMMENT '策略实例ID',
    signal_id VARCHAR(128) DEFAULT NULL COMMENT '信号唯一ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    strategy_symbol VARCHAR(32) NOT NULL COMMENT '策略交易对',
    intent_direction VARCHAR(16) NOT NULL COMMENT '意图方向：LONG 做多 SHORT 做空 CLOSE 平仓 ',
    signal_price DECIMAL(20,8) NOT NULL COMMENT '信号价格',
    generated_at DATETIME NOT NULL COMMENT '信号产生时间',
    received_at DATETIME NOT NULL COMMENT '系统接收时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注/调试信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='信号意图记录表';


CREATE TABLE strategy_rules_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略计算规则记录表ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    strategy_instance_id BIGINT NOT NULL COMMENT '策略实例ID',
    signal_id VARCHAR(128) DEFAULT NULL COMMENT '信号唯一ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    strategy_symbol VARCHAR(32) NOT NULL COMMENT '策略交易对',
    runtime_rules JSON COMMENT '策略运行规则结果记录',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='策略规则计算记录表';


CREATE TABLE strategy_logic_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略运行状态表ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    strategy_instance_id BIGINT NOT NULL COMMENT '策略实例ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    strategy_symbol VARCHAR(32) NOT NULL COMMENT '策略交易对',
    logic_position_side VARCHAR(16) NOT NULL COMMENT '逻辑持仓方向:LONG做多 SHORT做空 FLAT空仓',
    logic_position_qty DECIMAL(20,8) NOT NULL COMMENT '逻辑持仓数量（策略计算结果）',
    avg_entry_price DECIMAL(20,8) DEFAULT NULL COMMENT '逻辑平均开仓价',
    state_phase VARCHAR(32) NOT NULL COMMENT '策略阶段:IDLE 空闲 OPEN_PENDING 已决策待执行 OPENED 已开仓 PARTIAL_EXIT部分减仓 EXIT_PENDING 部分加仓 ADD_PENDING 已决策待平仓 CLOSED  已平仓',
    unrealized_pnl DECIMAL(20,8) DEFAULT NULL COMMENT '未实现盈亏',
    realized_pnl DECIMAL(20,8) DEFAULT NULL COMMENT '已实现盈亏',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='策略逻辑状态快照表（防止策略失忆）';


CREATE TABLE strategy_intent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '策略决策记录ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    strategy_instance_id BIGINT NOT NULL COMMENT '策略实例ID',
    strategy_logic_state_id BIGINT NOT NULL COMMENT '策略运行状态表ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    strategy_symbol VARCHAR(32) NOT NULL COMMENT '策略交易对',
    intent_action VARCHAR(32) NOT NULL COMMENT '决策意图:OPEN（开仓） CLOSE（关仓） ADD（加仓） REDUCE（减仓） REVERSE（反转）',
    intent_direction  VARCHAR(16) NOT NULL COMMENT '意图方向：LONG 做多 SHORT 做空 CLOSE 平仓',
    calculated_qty DECIMAL(20,8) NOT NULL COMMENT '策略计算出的下单数量',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL COMMENT '决策时间'
) COMMENT='策略决策流水表（只记录“我决定做什么”）';










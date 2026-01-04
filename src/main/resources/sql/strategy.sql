
CREATE TABLE `strategy_definition` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '策略ID',
  `user_id` bigint(20) NOT NULL COMMENT '所属用户ID',
  `strategy_name` varchar(64) NOT NULL COMMENT '策略名称',
  `strategy_type` varchar(32) NOT NULL COMMENT '\r\n    策略类型:\r\n    SIGNAL_DRIVEN  信号驱动\r\n    INDICATOR_DRIVEN 指标/因子驱动\r\n    HYBRID 混合策略\r\n    ',
  `decision_mode` varchar(64) DEFAULT NULL COMMENT '策略行为:\r\nFOLLOW_SIGNAL   完全跟随信号，信号即指令\r\nINTENT_DRIVEN   信号作为意图，由策略自行决定是否/何时兑现',
  `enabled` tinyint(4) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_name` (`strategy_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='策略定义表（描述策略是什么）';

CREATE TABLE strategy_param (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '参数ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',

    initial_capital DECIMAL(20,8) NOT NULL COMMENT '策略初始虚拟资金（用于内部收益曲线计算）',
    base_order_ratio DECIMAL(10,4) NOT NULL COMMENT '单次下单资金占比',

    take_profit_ratio DECIMAL(10,4) COMMENT '策略止盈比例（兜底型）',
    stop_loss_ratio DECIMAL(10,4) COMMENT '策略止损比例（兜底型）',

    entry_condition JSON COMMENT '
    策略入场条件（结构化表达）:
    示例:
    {
      "mode": "ANY", 
      "rules": [
        {"factor": "PRICE", "operator": "GT", "value": 65000},
        {"factor": "RSI_14", "operator": "LT", "value": 70}
      ]
    }
    ',

    exit_condition JSON COMMENT '
    策略退出条件（结构化表达）:
    示例:
    {
      "mode": "ANY",
      "rules": [
        {"type": "TAKE_PROFIT", "ratio": 0.05},
        {"type": "STOP_LOSS", "ratio": 0.02}
      ]
    }
    ',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    UNIQUE KEY uk_strategy (strategy_id)
) COMMENT='策略参数表（策略决策所需的全部参数）';


CREATE TABLE strategy_symbol (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',

    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_strategy_symbol (strategy_id, trading_pair_id)
) COMMENT='策略运行的交易对列表';


CREATE TABLE strategy_signal_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订阅ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    signal_config_id BIGINT NOT NULL COMMENT '信号配置ID',

    consume_mode VARCHAR(32) NOT NULL COMMENT '
    消费模式:
    LATEST_ONLY 只取最新有效信号
    ',

    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',

    UNIQUE KEY uk_strategy_signal (strategy_id, signal_config_id)
) COMMENT='策略订阅的信号配置';


CREATE TABLE signal_intent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '意图ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',
    signal_id VARCHAR(128) DEFAULT NULL COMMENT '信号唯一ID（如TV alert id）',
    intent_direction VARCHAR(16) NOT NULL COMMENT '意图方向：BUY / SELL / FLAT / REVERSE',
    intent_status VARCHAR(16) NOT NULL COMMENT '
意图状态:
ACTIVE   当前有效意图
CONSUMED 已被策略兑现
EXPIRED  被后续反向/新意图覆盖
IGNORED  被策略明确忽略
',
    generated_at DATETIME NOT NULL COMMENT '信号产生时间',
    received_at DATETIME NOT NULL COMMENT '系统接收时间',
    expired_at DATETIME DEFAULT NULL COMMENT '失效时间',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注/调试信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_strategy_pair (strategy_id, trading_pair_id)
) COMMENT='信号意图表（LATEST_ONLY 意图模型）';


CREATE TABLE strategy_logic_state (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '状态ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',

    logic_position_side VARCHAR(16) NOT NULL COMMENT '
逻辑持仓方向:
LONG
SHORT
FLAT
',

    logic_position_qty DECIMAL(20,8) NOT NULL COMMENT '逻辑持仓数量（策略计算结果）',
    avg_entry_price DECIMAL(20,8) DEFAULT NULL COMMENT '逻辑平均开仓价',

    state_phase VARCHAR(32) NOT NULL COMMENT '
策略阶段:
IDLE            空闲
OPEN_PENDING    已决策待执行
OPENED          已开仓
PARTIAL_EXIT    部分减仓
EXIT_PENDING    已决策待平仓
CLOSED          已平仓
',

    last_signal_intent_id BIGINT DEFAULT NULL COMMENT '最近一次关联的 signal_intent',

    unrealized_pnl DECIMAL(20,8) DEFAULT NULL COMMENT '未实现盈亏（策略内部估算）',
    realized_pnl DECIMAL(20,8) DEFAULT NULL COMMENT '已实现盈亏',

    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_strategy_pair (strategy_id, trading_pair_id)
) COMMENT='策略逻辑状态快照表（防止策略失忆）';


CREATE TABLE strategy_intent_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '决策记录ID',
    user_id bigint(20) NOT NULL COMMENT '所属用户ID',
    strategy_id BIGINT NOT NULL COMMENT '策略ID',
    trading_pair_id BIGINT NOT NULL COMMENT '交易对ID',

    signal_id BIGINT COMMENT '触发决策的信号ID',

    intent_action VARCHAR(32) NOT NULL COMMENT '
    决策意图:
    OPEN
    CLOSE
    ADD
    REDUCE
    REVERSE
    HOLD
    ',

    calculated_qty DECIMAL(20,8) NOT NULL COMMENT '策略计算出的下单数量',

    decision_reason VARCHAR(255) COMMENT '决策原因说明',

    created_at DATETIME NOT NULL COMMENT '决策时间'
) COMMENT='策略决策流水表（只记录“我决定做什么”）';











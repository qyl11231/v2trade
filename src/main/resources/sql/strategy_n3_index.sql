-- N3 阶段：策略逻辑状态表索引和约束

-- 添加唯一约束（确保一个 instance 只能有一条当前快照）
-- 注意：如果表已有数据，需要先清理重复数据
ALTER TABLE strategy_logic_state 
ADD UNIQUE KEY uk_instance (strategy_instance_id);

-- 添加索引（用于查询优化）
CREATE INDEX idx_strategy_logic_state_user_id ON strategy_logic_state(user_id);
CREATE INDEX idx_strategy_logic_state_strategy_id ON strategy_logic_state(strategy_id);
CREATE INDEX idx_strategy_logic_state_trading_pair_id ON strategy_logic_state(trading_pair_id);
CREATE INDEX idx_strategy_logic_state_phase ON strategy_logic_state(state_phase);


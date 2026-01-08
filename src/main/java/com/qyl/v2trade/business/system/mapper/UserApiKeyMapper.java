package com.qyl.v2trade.business.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qyl.v2trade.business.system.model.entity.UserApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户交易所API Key Mapper接口
 */
@Mapper
public interface UserApiKeyMapper extends BaseMapper<UserApiKey> {
}

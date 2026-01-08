package com.qyl.v2trade.business.system.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户交易所API Key实体
 */
@Data
@TableName("user_api_key")
public class UserApiKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * API Key ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID
     */
    private Long userId;

    /**
     * 交易所，如 OKX
     */
    private String exchange;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * Secret Key
     */
    private String secretKey;

    /**
     * passphrase
     */
    private String passphrase;

    /**
     * API Key名称
     */
    @TableField("api_key_name")
    private String apiKeyName;

    /**
     * 状态：1-启用 0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

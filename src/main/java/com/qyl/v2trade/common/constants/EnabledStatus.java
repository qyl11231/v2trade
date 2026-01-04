package com.qyl.v2trade.common.constants;

/**
 * 启用状态常量
 * 用于统一管理系统中 enabled 字段的值
 */
public class EnabledStatus {

    /**
     * 禁用
     */
    public static final Integer DISABLED = 0;

    /**
     * 启用
     */
    public static final Integer ENABLED = 1;

    private EnabledStatus() {
        // 工具类，禁止实例化
    }
}

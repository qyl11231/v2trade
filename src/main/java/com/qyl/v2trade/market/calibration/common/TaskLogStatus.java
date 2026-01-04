package com.qyl.v2trade.market.calibration.common;

/**
 * 任务执行状态常量
 */
public class TaskLogStatus {

    /**
     * 执行中
     */
    public static final String RUNNING = "RUNNING";

    /**
     * 成功
     */
    public static final String SUCCESS = "SUCCESS";

    /**
     * 失败
     */
    public static final String FAILED = "FAILED";

    private TaskLogStatus() {
        // 工具类，禁止实例化
    }
}


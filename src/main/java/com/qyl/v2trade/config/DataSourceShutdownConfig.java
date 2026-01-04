package com.qyl.v2trade.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;

/**
 * 数据源关闭配置
 * 处理应用关闭事件，确保数据源被正确关闭
 */
@Configuration
public class DataSourceShutdownConfig {

    @Autowired
    @Qualifier("mysqlDataSource")
    private DataSource mysqlDataSource;

    @Autowired
    @Qualifier("questDbDataSource")
    private DataSource questDbDataSource;

    /**
     * 应用关闭时的处理
     * 优雅地关闭数据源，避免连接池在应用运行期间被错误关闭
     */
    @EventListener
    public void handleContextClosed(ContextClosedEvent event) {
        // 在应用关闭时才关闭数据源
        // 通过Spring管理数据源的生命周期，避免在应用运行期间被错误关闭
        if (mysqlDataSource instanceof HikariDataSource) {
            ((HikariDataSource) mysqlDataSource).close();
        }
        
        if (questDbDataSource instanceof HikariDataSource) {
            ((HikariDataSource) questDbDataSource).close();
        }
    }
}
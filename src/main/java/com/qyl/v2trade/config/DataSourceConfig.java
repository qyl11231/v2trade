package com.qyl.v2trade.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 数据源配置
 * 
 * 明确区分：
 * - MySQL数据源（@Primary）：用于业务数据（market_subscription_config等）
 * - QuestDB数据源：仅用于行情数据存储和查询
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL数据源配置（主数据源）
     * 用于所有业务数据表，包括：
     * - market_subscription_config
     * - trading_pair
     * - exchange_market_pair
     * - 其他业务表
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * MySQL数据源（主数据源）
     * MyBatis-Plus会自动使用此数据源
     */
    @Bean
    @Primary
    public DataSource mysqlDataSource() {
        DataSourceProperties properties = mysqlDataSourceProperties();
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        
        // 连接池配置
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000); // 10分钟空闲超时
        config.setMaxLifetime(1800000); // 30分钟最大生命周期
        
        // 连接测试和验证
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        
        // 连接池名称（便于监控）
        config.setPoolName("MySQL-HikariPool");
        
        // 防止连接池在应用关闭时被错误关闭的配置
        config.setLeakDetectionThreshold(60000); // 1分钟泄漏检测
        config.setInitializationFailTimeout(1); // 初始化失败超时
        
        return new HikariDataSource(config);
    }
}
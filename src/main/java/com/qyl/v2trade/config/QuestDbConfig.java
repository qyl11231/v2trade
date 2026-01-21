package com.qyl.v2trade.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * QuestDB 数据源配置
 * 
 * 注意：
 * 1. QuestDB使用独立的连接池，与MySQL数据源完全隔离
 * 2. 使用HikariCP连接池提升性能，避免频繁创建连接
 * 3. 不会影响Spring Boot自动配置的MySQL数据源（@Primary在MySQL侧）
 * 4. 该数据源仅用于行情数据存储和查询，不用于业务数据
 */
@Configuration
public class QuestDbConfig {

    @Value("${questdb.url}")
    private String questDbUrl;

    @Value("${questdb.username:admin}")
    private String username;

    @Value("${questdb.password:quest}")
    private String password;

    @Value("${questdb.pool.max-active:10}")
    private int maxPoolSize;

    @Value("${questdb.pool.min-idle:2}")
    private int minIdle;

    @Value("${questdb.pool.max-wait:30000}")
    private long maxWaitMillis;

    /**
     * 创建QuestDB数据源（使用HikariCP连接池）
     * 
     * 性能优化说明：
     * - 使用连接池避免频繁创建/关闭连接
     * - 独立的数据源，不影响MySQL业务数据源
     * - 连接池大小根据实际负载调整
     */
    @Bean(name = "questDbDataSource")
    public DataSource questDbDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(questDbUrl);
        config.setUsername(username);
        config.setPassword(password);
        // QuestDB使用PostgreSQL协议，所以使用PostgreSQL驱动
        // QuestDB的JDBC连接通过PostgreSQL协议实现
        config.setDriverClassName("org.postgresql.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(maxWaitMillis);
        config.setIdleTimeout(600000); // 10分钟空闲超时
        config.setMaxLifetime(1800000); // 30分钟最大生命周期
        
        // 连接测试
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);
        
        // 连接池名称（便于监控）
        config.setPoolName("QuestDB-HikariPool");
        
        // 防止连接池在应用关闭时被错误关闭的配置
        config.setLeakDetectionThreshold(60000); // 1分钟泄漏检测
        // 初始化失败超时设为-1，表示允许应用启动即使QuestDB不可用（连接会在使用时重试）
        // 这样应用可以正常启动，QuestDB连接会在需要时自动重试
        config.setInitializationFailTimeout(-1);
        
        return new HikariDataSource(config);
    }

    /**
     * QuestDB JdbcTemplate
     * 使用独立的数据源，不会与MySQL的JdbcTemplate冲突
     */
    @Bean(name = "questDbJdbcTemplate")
    public JdbcTemplate questDbJdbcTemplate() {
        return new JdbcTemplate(questDbDataSource());
    }
}
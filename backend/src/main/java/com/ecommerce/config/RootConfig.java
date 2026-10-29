package com.ecommerce.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

@Configuration
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
@ComponentScan(basePackages = {
        "com.ecommerce.dao",
        "com.ecommerce.service",
        "com.ecommerce.scheduler",
        "com.ecommerce.config"
})
@EnableTransactionManagement
@EnableScheduling
public class RootConfig {

    private static final Logger log = LoggerFactory.getLogger(RootConfig.class);

    @Value("${db.driver:org.h2.Driver}")
    private String dbDriver;

    @Value("${db.url:jdbc:h2:file:./data/ecommerce_tracking_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;AUTO_SERVER=TRUE}")
    private String dbUrl;

    @Value("${db.username:sa}")
    private String dbUsername;

    @Value("${db.password:}")
    private String dbPassword;

    @Value("${hibernate.dialect:org.hibernate.dialect.H2Dialect}")
    private String hibernateDialect;

    @Value("${hibernate.hbm2ddl.auto:update}")
    private String hbm2ddlAuto;

    @Value("${hibernate.show_sql:false}")
    private String showSql;

    @Value("${hibernate.cache.use_second_level_cache:true}")
    private String useSecondLevelCache;

    @Value("${hibernate.cache.use_query_cache:true}")
    private String useQueryCache;

    @Value("${hibernate.generate_statistics:true}")
    private String generateStatistics;

    private boolean usingFallbackH2 = false;

    @Bean
    public DataSource dataSource() {
        // Attempt configured database (e.g. MySQL if DB_URL is set or local MySQL 3306 if reachable)
        if (System.getenv("DB_URL") != null && !System.getenv("DB_URL").isBlank()) {
            return createHikariDataSource(dbDriver, dbUrl, dbUsername, dbPassword);
        }

        // Check if local MySQL on port 3306 is accessible with root / empty or configured credentials
        String mysqlTryUrl = "jdbc:mysql://localhost:3306/ecommerce_tracking_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String mysqlUser = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : "root";
        String mysqlPass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "root";
        try {
            HikariConfig testCfg = new HikariConfig();
            testCfg.setDriverClassName("com.mysql.cj.jdbc.Driver");
            testCfg.setJdbcUrl(mysqlTryUrl);
            testCfg.setUsername(mysqlUser);
            testCfg.setPassword(mysqlPass);
            testCfg.setConnectionTimeout(2000);
            testCfg.setInitializationFailTimeout(2000);
            HikariDataSource ds = new HikariDataSource(testCfg);
            try (Connection conn = ds.getConnection()) {
                log.info("Successfully connected to local MySQL Server at {}", mysqlTryUrl);
                this.usingFallbackH2 = false;
                return ds;
            }
        } catch (Exception e) {
            log.info("Local MySQL credentials not set or server requires custom DB_PASSWORD; using H2 in MySQL compatibility mode ({}). Set DB_URL / DB_USERNAME / DB_PASSWORD in environment to use external MySQL.", dbUrl);
            this.usingFallbackH2 = true;
            return createHikariDataSource(dbDriver, dbUrl, dbUsername, dbPassword);
        }
    }

    private DataSource createHikariDataSource(String driver, String url, String user, String pass) {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("EcommerceHikariPool");
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.ecommerce.entity");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Properties props = new Properties();
        if (!usingFallbackH2 && (dbUrl.contains("mysql") || System.getenv("DB_URL") == null)) {
            props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        } else {
            props.put("hibernate.dialect", hibernateDialect);
        }
        props.put("hibernate.hbm2ddl.auto", hbm2ddlAuto);
        props.put("hibernate.show_sql", showSql);
        props.put("hibernate.format_sql", "false");

        // Hibernate Second-Level Cache & Query Cache (JCache + Caffeine provider)
        props.put("hibernate.cache.use_second_level_cache", useSecondLevelCache);
        props.put("hibernate.cache.use_query_cache", useQueryCache);
        props.put("hibernate.cache.region.factory_class", "jcache");
        props.put("hibernate.javax.cache.provider", "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        props.put("hibernate.javax.cache.missing_cache_strategy", "create");
        props.put("hibernate.generate_statistics", generateStatistics);

        em.setJpaProperties(props);
        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        JpaTransactionManager txManager = new JpaTransactionManager();
        txManager.setEntityManagerFactory(emf);
        return txManager;
    }
}


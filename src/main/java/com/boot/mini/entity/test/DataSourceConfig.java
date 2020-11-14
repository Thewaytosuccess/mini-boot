package com.boot.mini.entity.test;

import com.boot.mini.annotation.config.Configuration;
import com.boot.mini.annotation.config.ConfigurationProperties;

/**
 * @author xhzy
 */
@Configuration
@ConfigurationProperties(prefix = "spring.dataSource")
public class DataSourceConfig {

    private String url;

    private String jdbcDriver;

    private String userName;

    private String password;

    public String getUrl() {
        return url;
    }

    public String getJdbcDriver() {
        return jdbcDriver;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "DataSourceConfig{" +
                "url='" + url + '\'' +
                ", driver='" + jdbcDriver + '\'' +
                ", username='" + userName + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}

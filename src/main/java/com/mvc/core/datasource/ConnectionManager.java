package com.mvc.core.datasource;

import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.injection.ConfigurationProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;

/**
 * @author xhzy
 */
public class ConnectionManager {

    private static final ConnectionManager MANAGER = new ConnectionManager();

    private ConnectionManager(){}

    public static ConnectionManager getInstance(){ return MANAGER; }

    Connection connection;

    @PostConstruct
    private void init(){
        Map<String, Object> configs = ConfigurationProcessor.getInstance().getByPrefix("spring.dataSource");
        Object driver = configs.get("driver");
        if(Objects.nonNull(driver)){
            try {
                Class.forName(String.valueOf(driver));
            } catch (ClassNotFoundException e) {
                throw new ExceptionWrapper(e);
            }
        }

        Object url = configs.get("url");
        Object username = configs.get("username");
        Object password = configs.get("password");
        if(Objects.nonNull(url) && Objects.nonNull(password) && Objects.nonNull(username)){
            try {
                this.connection = DriverManager.getConnection(String.valueOf(url), String.valueOf(username),
                        String.valueOf(password));
            } catch (SQLException e) {
                throw new ExceptionWrapper(e);
            }
        }

    }

    public Connection getConnection(){
        if(Objects.isNull(connection)){
            init();
        }
        return this.connection;
    }

    @PreDestroy
    private void destroy(){
        if(Objects.nonNull(connection)){
            try {
                connection.close();
            } catch (SQLException e) {
                throw new ExceptionWrapper(e);
            }
        }
    }
}

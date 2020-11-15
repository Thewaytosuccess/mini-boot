package com.boot.mini.core.datasource.connection;

import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.core.injection.ConfigurationProcessor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * @author xhzy
 */
public class ConnectionManager {

    private static final ConnectionManager MANAGER = new ConnectionManager();

    private ConnectionManager(){}

    public static ConnectionManager getInstance(){ return MANAGER; }

    private Connection connection;

    private final List<String> prefix = Collections.unmodifiableList(Arrays.asList("spring.dataSource.druid", "spring.dataSource"));

    @PostConstruct
    public void init(){
        ConfigurationProcessor instance = ConfigurationProcessor.getInstance();
        Optional<String> first = prefix.stream().filter(e -> !instance.getByPrefix(e).isEmpty()).findFirst();
        if(!first.isPresent()){
            return;
        }
        Map<String, Object> configs = instance.getByPrefix(first.get());
        Object driverClassName = configs.get("driverClassName");
        Object driver = Objects.nonNull(driverClassName) ? driverClassName : configs.get("driver");

        boolean druid = first.get().equals(prefix.get(0));
        if(!druid && Objects.nonNull(driver)){
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
                this.connection = druid ? DruidDataSourceFactory.createDataSource(configs).getConnection() :
                        DriverManager.getConnection(String.valueOf(url), String.valueOf(username), String.valueOf(password));
                System.out.println("==== connect mysql success =====");
            } catch (Exception e) {
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
    public void destroy(){
        if(Objects.nonNull(connection)){
            try {
                connection.close();
            } catch (SQLException e) {
                throw new ExceptionWrapper(e);
            }
        }
    }
}

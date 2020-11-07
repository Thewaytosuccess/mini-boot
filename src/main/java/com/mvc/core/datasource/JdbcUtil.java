package com.mvc.core.datasource;

import com.mvc.core.exception.ExceptionWrapper;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author xhzy
 */
public class JdbcUtil<T> {

    private Connection connection;

    private Connection getConnection(){
        if(Objects.isNull(connection)){
            connection = ConnectionManager.getInstance().getConnection();
        }
        return connection;
    }

    public boolean update(String sql){
        try {
            return getConnection().prepareStatement(sql).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ExceptionWrapper(e);
        }
    }

    public List<T> query(Class<T> clazz, String sql){
        List<T> result = new ArrayList<>();
        try {
            List<Field> fields = DataSourceManager.getInstance().getTableMap().get(clazz);
            ResultSet resultSet = getConnection().prepareStatement(sql).executeQuery();
            while(resultSet.next()){
                T t = clazz.newInstance();
                fields.forEach(e -> {
                    try {
                        clazz.getDeclaredMethod(setter(e.getName()),e.getType()).invoke(t,
                                resultSet.getObject(DataSourceManager.getInstance().mapCamelCaseToUnderscore(
                                        e.getName()), e.getType()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                result.add(t);
            }
            return result;
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    private String setter(String name) {
        String setter = "set"+name.substring(0,1).toUpperCase();
        if(name.length() > 1){
            setter += name.substring(1);
        }
        return setter;
    }
}

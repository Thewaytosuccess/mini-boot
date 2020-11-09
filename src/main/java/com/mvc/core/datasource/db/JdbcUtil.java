package com.mvc.core.datasource.db;

import com.mvc.core.datasource.connection.ConnectionManager;
import com.mvc.core.datasource.db.DataSourceManager;
import com.mvc.core.datasource.db.generator.TableGenerator;
import com.mvc.core.exception.ExceptionWrapper;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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

    public boolean exist(String sql){
        try {
            return getConnection().prepareStatement(sql).executeQuery().next();
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
                fields.forEach(f -> {
                    try {
                        String column = TableGenerator.getInstance().mapCamelCaseToUnderscore(
                                f.getName());
                        Object value;
                        if(f.getType() == Date.class){
                            value = new Date(resultSet.getTimestamp(column).getTime());
                        }else{
                            value = resultSet.getObject(column, f.getType());
                        }
                        clazz.getDeclaredMethod(setter(f.getName()),f.getType()).invoke(t, value);
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

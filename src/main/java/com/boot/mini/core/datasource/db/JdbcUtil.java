package com.boot.mini.core.datasource.db;

import com.boot.mini.core.datasource.connection.ConnectionManager;
import com.boot.mini.core.datasource.db.generator.TableGenerator;
import com.boot.mini.core.exception.ExceptionWrapper;

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

    private static Connection connection;

    private static Connection getConnection(){
        if(Objects.isNull(connection)){
            connection = ConnectionManager.getInstance().getConnection();
        }
        return connection;
    }

    public static boolean update(String sql){
        try {
            return getConnection().prepareStatement(sql).executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ExceptionWrapper(e);
        }
    }

    public static boolean exist(String sql){
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
                        String column = TableGenerator.getInstance().mapCamelCaseToUnderscore(f.getName());
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

    public static String setter(String name) {
        String setter = "set"+name.substring(0,1).toUpperCase();
        if(name.length() > 1){
            setter += name.substring(1);
        }
        return setter;
    }
}

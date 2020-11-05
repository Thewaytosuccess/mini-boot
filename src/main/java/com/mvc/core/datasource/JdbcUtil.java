package com.mvc.core.datasource;

import com.mvc.core.exception.ExceptionWrapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xhzy
 */
public class JdbcUtil {

    Connection connection;

    public JdbcUtil(){
        connection = ConnectionManager.getInstance().getConnection();
    }

    public boolean save(String sql){
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setBoolean(1,true);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ExceptionWrapper(e);
        }
    }

    public List<Object> get(String sql){
        List<Object> result = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setBoolean(1,true);
            ResultSet resultSet = ps.executeQuery();
            while(resultSet.next()){
                resultSet.getObject("columnLabel",Object.class);
            }
            return result;
        } catch (SQLException e) {
            throw new ExceptionWrapper(e);
        }
    }
}

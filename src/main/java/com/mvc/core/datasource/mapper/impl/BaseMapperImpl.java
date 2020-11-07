package com.mvc.core.datasource.mapper.impl;

import com.mvc.core.datasource.JdbcUtil;
import com.mvc.core.datasource.SqlGenerator;
import com.mvc.core.datasource.mapper.BaseMapper;
import com.mvc.enums.SqlTypeEnum;

import java.util.List;

/**
 * @author xhzy
 */
public class BaseMapperImpl<T> implements BaseMapper<T> {

    @Override
    public boolean insert(T t) {
        String sql = SqlGenerator.getInstance().generate(t, SqlTypeEnum.INSERT);
        System.out.println("insert sql = "+sql);
        return new JdbcUtil<>().update(sql);
    }

    @Override
    public boolean deleteByPrimaryKey(T t) {
        String sql = SqlGenerator.getInstance().generate(t, SqlTypeEnum.DELETE);
        System.out.println("delete sql = "+sql);
        return new JdbcUtil<>().update(sql);
    }

    @Override
    public boolean updateByPrimaryKey(T t) {
        String sql = SqlGenerator.getInstance().generate(t, SqlTypeEnum.UPDATE);
        System.out.println("update sql = "+sql);
        return new JdbcUtil<>().update(sql);
    }

    @Override
    public List<T> select(T t) {
        String sql = SqlGenerator.getInstance().generate(t, SqlTypeEnum.SELECT);
        System.out.println("select sql = "+sql);
        JdbcUtil<T> util = new JdbcUtil<>();
        return util.query((Class<T>) t.getClass(), sql);
    }

}

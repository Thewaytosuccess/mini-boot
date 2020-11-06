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
        return new JdbcUtil<>().update(SqlGenerator.getInstance().generate(t, SqlTypeEnum.INSERT));
    }

    @Override
    public boolean deleteByPrimaryKey(T t) {
        return new JdbcUtil<>().update(SqlGenerator.getInstance().generate(t, SqlTypeEnum.DELETE));
    }

    @Override
    public boolean updateByPrimaryKey(T t) {
        return new JdbcUtil<>().update(SqlGenerator.getInstance().generate(t, SqlTypeEnum.UPDATE));
    }

    @Override
    public List<T> select(T t) {
        JdbcUtil<T> util = new JdbcUtil<>();
        return util.query((Class<T>) t.getClass(), SqlGenerator.getInstance().generate(t, SqlTypeEnum.SELECT));
    }

}

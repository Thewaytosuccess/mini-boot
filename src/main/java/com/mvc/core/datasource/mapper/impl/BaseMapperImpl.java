package com.mvc.core.datasource.mapper.impl;

import com.mvc.core.datasource.db.JdbcUtil;
import com.mvc.core.datasource.db.generator.SqlGenerator;
import com.mvc.core.datasource.mapper.BaseMapper;
import com.mvc.core.datasource.wrapper.impl.QueryWrapper;
import com.mvc.enums.SqlTypeEnum;

import java.util.List;
import java.util.Objects;

/**
 * @author xhzy
 */
public class BaseMapperImpl<T> implements BaseMapper<T> {

    private SqlGenerator<T> generator;

    public SqlGenerator<T> getSqlGenerator(){
        if(Objects.isNull(generator)){
            generator = new SqlGenerator<>();
        }
        return generator;
    }

    @Override
    public boolean insert(T t) {
        String sql = getSqlGenerator().generate(t, SqlTypeEnum.INSERT);
        System.out.println("insert sql = "+sql);
        return new JdbcUtil<>().update(sql);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean deleteByPrimaryKey(Object o, Class<T>... t) {
        String sql = getSqlGenerator().generate(o, SqlTypeEnum.DELETE,t);
        System.out.println("delete sql = "+sql);
        return new JdbcUtil<>().update(sql);
    }

    @Override
    public boolean updateByPrimaryKey(T t) {
        String sql = getSqlGenerator().generate(t, SqlTypeEnum.UPDATE);
        System.out.println("update sql = "+sql);
        return new JdbcUtil<>().update(sql);
    }

    @Override
    public List<T> select(QueryWrapper<T> t) {
        String sql = t.sql();
        System.out.println("select sql = "+sql);
        JdbcUtil<T> util = new JdbcUtil<>();
        return util.query(t.getGeneric(), sql);
    }

    @Override
    public T selectByPrimaryKey(Object o, Class<T>... t) {
        String sql = getSqlGenerator().generate(o, SqlTypeEnum.SELECT, t);
        System.out.println("select sql = "+sql);
        JdbcUtil<T> util = new JdbcUtil<>();
        List<T> list = util.query(t[0], sql);
        return list.isEmpty() ? null : list.get(0);
    }

}

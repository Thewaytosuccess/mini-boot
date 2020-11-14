package com.boot.mini.core.datasource.mapper;

import com.boot.mini.core.datasource.wrapper.impl.QueryWrapper;

import java.util.List;

/**
 * @author xhzy
 */
public interface BaseMapper<T> {

    /**
     * insert
     * @param obj obj
     * @return result
     */
    boolean insert(T obj);

    /**
     * delete
     * @param o obj
     * @param t generic type
     * @return result
     */
    boolean deleteByPrimaryKey(Object o,Class<T>... t);

    /**
     * update
     * @param obj obj
     * @return result
     */
    boolean updateByPrimaryKey(T obj);

    /**
     * select
     * @param obj obj
     * @return result
     */
    List<T> select(QueryWrapper<T> obj);

    /**
     * select
     * @param o obj
     * @param t obj
     * @return result
     */
    T selectByPrimaryKey(Object o, Class<T>... t);

}

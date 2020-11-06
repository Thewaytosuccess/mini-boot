package com.mvc.core.datasource.mapper;

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
     * @param obj obj
     * @return result
     */
    boolean deleteByPrimaryKey(T obj);

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
    List<T> select(T obj);

}

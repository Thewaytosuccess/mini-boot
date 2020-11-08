package com.mvc.core.datasource.wrapper;

import com.mvc.core.datasource.wrapper.impl.QueryWrapper;

/**
 * @author xhzy
 */
public interface Wrapper<T> {

    /**
     * find eq
     * @param column column
     * @param value value
     * @return wrapper
     */
    QueryWrapper<T> eq(String column, Object value);

    /**
     * find like
     * @param column column
     * @param value value
     * @return wrapper
     */
    QueryWrapper<T> like(String column,Object value);

    /**
     * find ge
     * @param column column
     * @param value value
     * @return wrapper
     */
    QueryWrapper<T> ge(String column,Object value);

    /**
     * find le
     * @param column column
     * @param value value
     * @return wrapper
     */
    QueryWrapper<T> le(String column,Object value);

    /**
     * find gt
     * @param column column
     * @param value value
     * @return wrapper
     */
    QueryWrapper<T> gt(String column,Object value);

    /**
     * find lt
     * @param column column
     * @param value value
     * @return wrapper
     */
    QueryWrapper<T> lt(String column,Object value);

    /**
     * order by desc
     * @param column column
     * @return wrapper
     */
    QueryWrapper<T> orderByDesc(String column);

    /**
     * order by asc
     * @param column column
     * @return wrapper
     */
    QueryWrapper<T> orderByAsc(String column);

    /**
     * by page
     * @param current pageNo
     * @param size pageSize
     * @return wrapper
     */
    QueryWrapper<T> limit(int current,int size);


}

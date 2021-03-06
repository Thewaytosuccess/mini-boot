package com.boot.mini.core.datasource.wrapper.impl;

import com.boot.mini.annotation.jpa.Id;
import com.boot.mini.annotation.jpa.PrimaryKey;
import com.boot.mini.core.datasource.db.DataSourceManager;
import com.boot.mini.core.datasource.db.generator.TableGenerator;
import com.boot.mini.core.datasource.wrapper.Wrapper;
import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.core.util.DateUtil;
import com.boot.mini.enums.ExceptionEnum;
import com.boot.mini.enums.constant.ConstantPool;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class QueryWrapper<T> implements Wrapper<T> {

    private StringBuilder builder;

    public QueryWrapper(){
        Class<T> t = getGeneric();
        if(Objects.isNull(t)){
            throw new ExceptionWrapper(ExceptionEnum.GENERIC_ERROR);
        }

        String table = TableGenerator.getInstance().getTableName(t);
        if(Objects.isNull(table)){
            throw new ExceptionWrapper(ExceptionEnum.TABLE_NULL);
        }

        List<Field> columns = DataSourceManager.getInstance().getTableMap().get(t);
        Field[] fields = t.getDeclaredFields();
        if(columns.size() != fields.length){
            columns.addAll(Arrays.stream(fields).filter(e -> !e.isAnnotationPresent(Id.class) && !e.isAnnotationPresent(PrimaryKey.class))
                    .collect(Collectors.toSet()));
        }
        this.builder = new StringBuilder("select ").append(columns.stream().map(this::getColumn).collect(Collectors.joining(",")))
                .append(" from ").append(table).append(" where 1 = 1 and");
    }

    public String sql(){
        if(builder.toString().endsWith(ConstantPool.AND)){
            return builder.toString().substring(0,builder.length() - 3);
        }
        return builder.toString();
    }

    private String getColumn(Field f){ return TableGenerator.getInstance().getColumnName(f); }

    @Override
    public QueryWrapper<T> eq(String column,Object value){
        builder.append(" ").append(column).append(" = ");
        if(value instanceof String){
            builder.append("'").append(value).append("'");
        }else{
            builder.append(value);
        }
        builder.append(" and");
        return this;
    }

    @Override
    public QueryWrapper<T> like(String column,Object value){
        if(value instanceof String) {
            builder.append(" ").append(column).append(" like ").append("'%").append(value).append("%'").append(" and");
        }
        return this;
    }

    @Override
    public QueryWrapper<T> ge(String column,Object value){
        compare(column,value,">=");
        return this;
    }

    @Override
    public QueryWrapper<T> le(String column,Object value){
        compare(column,value,"<=");
        return this;
    }

    @Override
    public QueryWrapper<T> gt(String column,Object value){
        compare(column,value,">");
        return this;
    }

    @Override
    public QueryWrapper<T> lt(String column,Object value){
        compare(column,value,"<");
        return this;
    }

    private void compare(String column,Object value,String sign){
        if(value instanceof Date){
            Date date = (Date)value;
            builder.append(" datediff(").append(column).append(",").append(DateUtil.format(date))
                    .append(") ").append(sign).append(" 0 and");
        }else if(!(value instanceof String)){
            builder.append(" ").append(column).append(" ").append(sign).append(" ").append(value).append(" and");
        }
    }

    @Override
    public QueryWrapper<T> orderByDesc(String column){
        orderBy();
        builder.append(column).append(" desc,");
        return this;
    }

    @Override
    public QueryWrapper<T> orderByAsc(String column){
        orderBy();
        builder.append(column).append(" asc,");
        return this;
    }

    private void orderBy(){
        if(builder.toString().endsWith(ConstantPool.AND)){
            builder = new StringBuilder(builder.toString().substring(0,builder.length() - 3));
        }
        if(!builder.toString().contains(ConstantPool.ORDER)){
            builder.append(" order by ");
        }
    }

    @Override
    public QueryWrapper<T> limit(int current,int size){
        if(builder.toString().endsWith(ConstantPool.COMMA)){
            builder.deleteCharAt(builder.length() - 1);
        }
        if(current < 1){
            current = 1;
        }
        int start = (current - 1) * size;
        builder.append(" limit ").append(start).append(",").append(start + size);
        return this;
    }

    public Class<T> getGeneric() {
        Type t = getClass().getGenericSuperclass();
        if(t instanceof ParameterizedType){
            ParameterizedType type = (ParameterizedType)t;
            return (Class<T>) type.getActualTypeArguments()[0];
        }
        return null;
    }

}

package com.mvc.core.datasource.db.generator;

import com.mvc.core.datasource.db.DataSourceManager;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.util.DateUtil;
import com.mvc.enums.ExceptionEnum;
import com.mvc.enums.SqlTypeEnum;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class SqlGenerator {

    private static final SqlGenerator GENERATOR = new SqlGenerator();

    private SqlGenerator(){}

    public static SqlGenerator getInstance(){ return GENERATOR; }

    public String generate(Object obj, SqlTypeEnum type){
        //todo
        List<Field> columns = DataSourceManager.getInstance().getTableMap().get(obj.getClass());
        if(Objects.nonNull(columns) && !columns.isEmpty()){
            switch (type){
                case INSERT: return insert(obj,columns);
                case DELETE: return deleteByPrimaryKey(obj,columns);
                case UPDATE: return updateByPrimaryKey(obj,columns);
                case SELECT: return selectByPrimaryKey(obj,columns);
                default:
                    throw new IllegalStateException("Unexpected value: " + type);
            }
        }
        return null;
    }

    private String insert(Object obj, List<Field> columns){
        Class<?> clazz = obj.getClass();
        String table = getTable(clazz);
        //todo generate pk
        StringBuilder builder = new StringBuilder("insert into " + table + " (").append(columns.stream()
                .map(this::getColumn).collect(Collectors.joining(","))).append(") values (");

        for(Field f:columns){
            try {
                if(f.getType() == String.class || f.getType() == Date.class){
                    builder.append("'");
                }
                Object value = clazz.getDeclaredMethod(getter(f.getName())).invoke(obj);
                if(Objects.nonNull(value) && value instanceof Date){
                    value = DateUtil.format((Date)value);
                }
                if(Objects.nonNull(value) && value instanceof Boolean){
                    value = (Boolean)value ? 1 : 0;
                }
                builder.append(value);
                if(f.getType() == String.class || f.getType() == Date.class){
                    builder.append("'");
                }
                builder.append(",");
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
        return builder.deleteCharAt(builder.length() - 1).append(")").toString();
    }

    private String getter(String columnName){
        String getter = "get" + columnName.substring(0,1).toUpperCase();
        if(columnName.length() > 1){
            getter += columnName.substring(1);
        }
        return getter;
    }

    private String getTable(Class<?> clazz){
        return TableGenerator.getInstance().getTableName(clazz);
    }

    private String getColumn(Field f){ return TableGenerator.getInstance().getColumnName(f); }

    private String deleteByPrimaryKey(Object obj, List<Field> columns){
        if(columns.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
        }
        Field pk = columns.get(0);
        Class<?> clazz = pk.getDeclaringClass();
        String table = getTable(clazz);
        StringBuilder builder = new StringBuilder("delete from " + table + " where ").append(getColumn(pk))
                .append(" = ");
        try {
            Object value = clazz.getDeclaredMethod(getter(pk.getName())).invoke(obj);
            return value instanceof String ? builder.append("'").append(value).append("'").toString() :
                    builder.append(value).toString();
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    private String updateByPrimaryKey(Object obj, List<Field> columns){
        Class<?> clazz = obj.getClass();
        String table = getTable(clazz);

        StringBuilder builder = new StringBuilder("update " + table + " set ");
        Object value;
        for(Field f:columns){
            try {
                value = clazz.getDeclaredMethod(getter(f.getName())).invoke(obj);
                if(Objects.nonNull(value)){
                    builder.append(getColumn(f)).append(" = ");
                    if(f.getType() == String.class){
                        builder.append("'");
                    }
                    builder.append(value);
                    if(f.getType() == String.class){
                        builder.append("'");
                    }
                    builder.append(",");
                }
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }

        Field pk = columns.get(0);
        builder.deleteCharAt(builder.length() - 1).append(" where ").append(getColumn(pk)).append(" = ");
        try {
            value = clazz.getDeclaredMethod(getter(pk.getName())).invoke(obj);
            return pk.getType() == String.class ? builder.append("'").append(value).append("'").toString() :
                    builder.append(value).toString();
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    private String selectByPrimaryKey(Object obj, List<Field> columns){
        if(columns.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
        }

        Field pk = columns.get(0);
        Class<?> clazz = pk.getDeclaringClass();
        String table = getTable(clazz);

        StringBuilder builder = new StringBuilder("select ").append(columns.stream().map(this::getColumn)
                .collect(Collectors.joining(","))).append(" from ").append(table).append(" where ");
        try {
            Object value = clazz.getDeclaredMethod(getter(pk.getName())).invoke(obj);
            if(Objects.nonNull(value)){
                builder.append(getColumn(pk)).append(" = ");
                return pk.getType() == String.class ? builder.append("'").append(value).append("'").toString() :
                        builder.append(value).toString();
            }else{
                throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
            }
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

}

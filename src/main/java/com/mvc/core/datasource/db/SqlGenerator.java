package com.mvc.core.datasource.db;

import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.util.DateUtil;
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
        List<Field> columns = DataSourceManager.getInstance().getTableMap().get(obj.getClass());
        if(Objects.nonNull(columns) && !columns.isEmpty()){
            switch (type){
                case INSERT: return insert(obj,columns);
                case DELETE: return deleteByPrimaryKey(obj,columns);
                case UPDATE: return updateByPrimaryKey(obj,columns);
                case SELECT: return select(obj,columns);
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
        return DataSourceManager.getInstance().getTableName(clazz);
    }

    private String getColumn(Field f){ return DataSourceManager.getInstance().getColumnName(f); }

    private String deleteByPrimaryKey(Object obj, List<Field> columns){
        Class<?> clazz = obj.getClass();
        String table = getTable(clazz);

        Field pk = columns.get(0);
        StringBuilder builder = new StringBuilder("delete from " + table + " where ").append(getColumn(pk))
                .append(" = ");
        try {
            return builder.append(clazz.getDeclaredMethod(getter(pk.getName())).invoke(obj)).toString();
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
            if(pk.getType() == String.class){
                builder.append("'");
            }
            builder.append(clazz.getDeclaredMethod(getter(pk.getName())).invoke(obj));
            if(pk.getType() == String.class){
                builder.append("'");
            }
            return builder.toString();
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    private String select(Object obj, List<Field> columns){
        Class<?> clazz = obj.getClass();
        String table = getTable(clazz);

        StringBuilder builder = new StringBuilder("select ").append(columns.stream().map(this::getColumn)
                .collect(Collectors.joining(","))).append(" from ").append(table).append(" where ");

        Field pk = columns.get(0);
        try {
            Object value = clazz.getDeclaredMethod(getter(pk.getName())).invoke(obj);
            if(Objects.nonNull(value)){
                builder.append(getColumn(pk)).append(" = ");
                if(pk.getType() == String.class){
                    builder.append("'");
                }
                builder.append(value);
                if(pk.getType() == String.class){
                    builder.append("'");
                }
                return builder.toString();
            }else{
                for (Field f:columns){
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
                            builder.append(" and ");
                        }
                    } catch (Exception e) {
                        throw new ExceptionWrapper(e);
                    }
                }
                return builder.deleteCharAt(builder.length() - 6).toString();
            }
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

}

package com.boot.mini.core.datasource.db.generator;

import com.boot.mini.annotation.jpa.PrimaryKey;
import com.boot.mini.core.datasource.db.DataSourceManager;
import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.core.util.DateUtil;
import com.boot.mini.entity.method.MethodInfo;
import com.boot.mini.enums.ExceptionEnum;
import com.boot.mini.enums.SqlTypeEnum;
import com.boot.mini.enums.constant.ConstantPool;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class SqlGenerator<T> {

    @SafeVarargs
    public final String generate(Object obj, SqlTypeEnum type, Class<T>... t){
        Class<?> clazz = Objects.nonNull(t) ? t[0] : obj.getClass();
        List<Field> columns = DataSourceManager.getInstance().getTableMap().get(clazz);
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
        if(columns.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
        }
        Class<?> clazz = obj.getClass();
        StringBuilder builder = new StringBuilder("insert into " + getTable(clazz) + " (").append(columns.stream()
                .map(this::getColumn).collect(Collectors.joining(","))).append(") values (");

        boolean isPrimaryKey = columns.get(0).getType() == String.class;
        Object value;
        for(Field f:columns){
            try {
                if(f.getType() == String.class || f.getType() == Date.class){
                    builder.append("'");
                }

                if(isPrimaryKey){
                    value = getId(f,obj);
                    isPrimaryKey = false;
                }else{
                    value = clazz.getDeclaredMethod(MethodInfo.getter(f.getName())).invoke(obj);
                }
                if(Objects.nonNull(value) && value instanceof Date){
                    value = DateUtil.format((Date)value);
                }
                if(Objects.nonNull(value) && value instanceof Boolean){
                    value = (boolean)value ? 1 : 0;
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

    private Object getId(Field f,Object o) throws Exception {
        String policy = f.getAnnotation(PrimaryKey.class).policy();
        if("random".equals(policy) && f.getAnnotation(PrimaryKey.class).idGenerator() == Void.class){
            String id = UUID.randomUUID().toString().replaceAll(ConstantPool.STRIKE_THROUGH, "");
            o.getClass().getDeclaredMethod(MethodInfo.setter(f.getName()),f.getType()).invoke(o,id);
            return id;
        }else{
            //todo generate pk
            return null;
        }
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
        StringBuilder builder = new StringBuilder("delete from " + getTable(pk.getDeclaringClass()) + " where ")
                .append(getColumn(pk)).append(" = ");
        return obj instanceof String ? builder.append("'").append(obj).append("'").toString() : builder.append(obj).toString();
    }

    private String updateByPrimaryKey(Object obj, List<Field> columns){
        if(columns.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
        }

        Field pk = columns.get(0);
        Class<?> clazz = pk.getDeclaringClass();
        StringBuilder builder = new StringBuilder("update " + getTable(clazz) + " set ");
        Object value;
        for(Field f:columns){
            try {
                value = clazz.getDeclaredMethod(MethodInfo.getter(f.getName())).invoke(obj);
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
        builder.deleteCharAt(builder.length() - 1).append(" where ").append(getColumn(pk)).append(" = ");
        try {
            value = clazz.getDeclaredMethod(MethodInfo.getter(pk.getName())).invoke(obj);
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
        StringBuilder builder = new StringBuilder("select ").append(columns.stream().map(this::getColumn)
                .collect(Collectors.joining(","))).append(" from ")
                .append(getTable(pk.getDeclaringClass())).append(" where ")
                .append(getColumn(pk)).append(" = ");
        return pk.getType() == String.class ? builder.append("'").append(obj).append("'").toString() :
                builder.append(obj).toString();
    }

}

package com.boot.mini.core.datasource.db.generator;

import com.boot.mini.annotation.jpa.Column;
import com.boot.mini.annotation.jpa.Id;
import com.boot.mini.annotation.jpa.PrimaryKey;
import com.boot.mini.annotation.jpa.Table;
import com.boot.mini.core.datasource.db.DataSourceManager;
import com.boot.mini.core.datasource.db.JdbcUtil;
import com.boot.mini.core.exception.ExceptionWrapper;
import com.boot.mini.enums.ExceptionEnum;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author xhzy
 */
public class TableGenerator {

    private static final TableGenerator GENERATOR = new TableGenerator();

    private TableGenerator(){}

    public static TableGenerator getInstance(){ return GENERATOR; }

    public String getColumnName(Field e) {
        if(!e.isAnnotationPresent(Column.class)){
            return mapCamelCaseToUnderscore(e.getName());
        }
        String columnName = e.getAnnotation(Column.class).column();
        return columnName.isEmpty() ? mapCamelCaseToUnderscore(e.getName()) : columnName;
    }

    public String getTableName(Class<?> clazz) {
        if(!clazz.isAnnotationPresent(Table.class)){
            return mapCamelCaseToUnderscore(clazz.getSimpleName());
        }
        String tableName = clazz.getAnnotation(Table.class).table();
        return tableName.isEmpty() ? mapCamelCaseToUnderscore(clazz.getSimpleName()) : tableName;
    }

    public String mapCamelCaseToUnderscore(String simpleName){
        StringBuilder builder = new StringBuilder(simpleName.substring(0,1).toLowerCase());
        char[] chars = simpleName.toCharArray();
        for(int i=1,len = simpleName.length(); i < len; ++i){
            while(i < len && (Character.isLowerCase(chars[i]))){
                builder.append(chars[i++]);
            }
            if(i < len){
                builder.append("_").append(Character.toLowerCase(chars[i]));
            }
        }
        return builder.toString();
    }

    public void createTable(Class<?> clazz) {
        if(!clazz.isAnnotationPresent(Table.class)){
            return;
        }
        Table table = clazz.getAnnotation(Table.class);
        if(table.create()){
            String tableName = getTableName(clazz);
            String sql = "select table_name from information_schema.tables where table_name = '" + tableName
                    +"' and table_schema = 'test'";
            if(!JdbcUtil.exist(sql)){
                List<Field> fields = DataSourceManager.getInstance().getTableMap().get(clazz);
                if(Objects.isNull(fields) || fields.isEmpty()){
                    return;
                }

                StringBuilder builder = new StringBuilder("create table " + tableName +" (");
                for (Field f : fields) {
                    Column column = f.getAnnotation(Column.class);
                    if (column.length() == 0) {
                        throw new ExceptionWrapper(ExceptionEnum.LENGTH_NULL);
                    }

                    builder.append(getColumnName(f));
                    if (f.isAnnotationPresent(Id.class) || f.isAnnotationPresent(PrimaryKey.class)) {
                        getDefinitionOfPrimaryKey(builder,column,f);
                        continue;
                    }

                    if (f.getType() == String.class) {
                        getDefinitionOfString(builder, column);
                    } else if (f.getType() == Integer.class) {
                        getDefinitionOfInteger(builder, column);
                    } else if (f.getType() == Short.class) {
                        getDefinitionOfShort(builder, column);
                    } else if (f.getType() == Boolean.class) {
                        getDefinitionOfBoolean(builder, column);
                    } else if (f.getType() == Date.class) {
                        getDefinitionOfDate(builder, column);
                    } else if (f.getType() == Long.class) {
                        getDefinitionOfLong(builder, column);
                    }

                    appendComment(builder,column);
                    builder.append(",");
                }
                builder.deleteCharAt(builder.length() - 1).append(")engine = InnoDB default charset = utf8mb4");
                if(!table.comment().isEmpty()){
                    builder.append(" comment '").append(table.comment()).append("'");
                }
                System.out.println("create sql = " + builder.toString());
                if(JdbcUtil.update(builder.toString())){
                    System.out.println("create table success [" + tableName + "]");
                }
            }
        }
    }

    private void getDefinitionOfPrimaryKey(StringBuilder builder,Column column,Field f){
        if(f.getType() == String.class){
            builder.append(" varchar(").append(column.length()).append(")");
        }else{
            builder.append(f.getType() == Long.class ? " bigint" : " int");
            appendUnsigned(builder, column);
        }

        builder.append(" primary key");
        if (f.isAnnotationPresent(Id.class) && f.getAnnotation(Id.class).autoIncrement()) {
            builder.append(" auto_increment");
        }
        builder.append(",");
    }

    private void getDefinitionOfDate(StringBuilder builder,Column column){
        builder.append(" datetime");
        appendNonNull(builder,column);
        appendDefault(builder, column);
    }

    private void getDefinitionOfBoolean(StringBuilder builder,Column column){
        builder.append(" bit(1)");
        appendNonNull(builder,column);
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(("true".equals(column.defaultValue()) ||
                    "1".equals(column.defaultValue())) ? 1 : 0);
        }
    }

    private void getDefinitionOfLong(StringBuilder builder,Column column){
        builder.append(" bigint(").append(column.length()).append(")");
        getCommonDefinition(builder,column);
    }

    private void getDefinitionOfShort(StringBuilder builder,Column column){
        builder.append(" tinyint(").append(column.length()).append(")");
        getCommonDefinition(builder,column);
    }

    private void getDefinitionOfInteger(StringBuilder builder,Column column){
        builder.append(column.length() <= 65535 ? " int(" : " mediumint(").append(column.length()).append(")");
        getCommonDefinition(builder,column);
    }

    private void getCommonDefinition(StringBuilder builder,Column column){
        appendUnsigned(builder,column);
        appendNonNull(builder,column);
        appendDefault(builder,column);
    }

    private void getDefinitionOfString(StringBuilder builder,Column column){
        builder.append(" varchar(").append(column.length()).append(")");
        appendNonNull(builder,column);
        if(!column.defaultValue().isEmpty()){
            builder.append(" default '").append(column.defaultValue()).append("'");
        }
    }

    private void appendDefault(StringBuilder builder,Column column){
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(column.defaultValue());
        }
    }

    private void appendComment(StringBuilder builder,Column column){
        if (!column.comment().isEmpty()) {
            builder.append(" comment '").append(column.comment()).append("'");
        }
    }

    private void appendUnsigned(StringBuilder builder,Column column){
        if(column.unsigned()){
            builder.append(" unsigned");
        }
    }

    private void appendNonNull(StringBuilder builder,Column column){
        if(column.nonnull()){
            builder.append(" not null");
        }
    }

}

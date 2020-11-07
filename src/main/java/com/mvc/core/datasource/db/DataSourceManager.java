package com.mvc.core.datasource.db;

import com.mvc.annotation.enable.EnableDataSourceAutoConfiguration;
import com.mvc.annotation.jpa.Column;
import com.mvc.annotation.jpa.Id;
import com.mvc.annotation.jpa.PrimaryKey;
import com.mvc.annotation.jpa.Table;
import com.mvc.core.datasource.connection.ConnectionManager;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.core.mapping.PackageScanner;
import com.mvc.enums.ExceptionEnum;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class DataSourceManager {

    private static final DataSourceManager MANAGER = new DataSourceManager();

    private DataSourceManager(){}

    public static DataSourceManager getInstance(){ return MANAGER; }

    private Map<Class<?>,List<Field>> tableMap;

    public Map<Class<?>, List<Field>> getTableMap() {
        if(Objects.isNull(tableMap)){
            tableMap = new HashMap<>(16);
        }
        return tableMap;
    }

    public void init(){
        PackageScanner packageScanner = PackageScanner.getInstance();
        AtomicBoolean enabled = new AtomicBoolean(false);
        Optional.ofNullable(packageScanner.getStarterClass()).ifPresent(e ->
                enabled.set(e.isAnnotationPresent(EnableDataSourceAutoConfiguration.class)));
        if(enabled.get()){
            Set<Class<?>> tables = packageScanner.getAllClasses().stream().filter(e -> e.isAnnotationPresent(Table.class))
                    .collect(Collectors.toSet());
            tables.forEach(this::getColumns);
            //try connect database
            ConnectionManager.getInstance().init();
            //create table
            tables.forEach(this::createTable);
        }
    }

    private void createTable(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if(table.create()){
            String tableName = getTableName(clazz);
            String sql = "select table_name from information_schema.tables where table_name = '" + tableName
                    +"' and table_schema = 'test'";
            JdbcUtil<Object> util = new JdbcUtil<>();
            if(!util.exist(sql)){
                List<Field> fields = tableMap.get(clazz);
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
                    if (!column.comment().isEmpty()) {
                        builder.append(" comment '").append(column.comment()).append("'");
                    }
                    builder.append(",");
                }
                builder.deleteCharAt(builder.length() - 1).append(")engine = InnoDB default charset = utf8mb4");
                if(!table.comment().isEmpty()){
                    builder.append(" comment '").append(table.comment()).append("'");
                }
                System.out.println("create sql = " + builder.toString());
                if(util.update(builder.toString())){
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
            if (column.unsigned()) {
                builder.append(" unsigned");
            }
        }

        builder.append(" primary key");
        if (f.isAnnotationPresent(Id.class) && f.getAnnotation(Id.class).autoIncrement()) {
            builder.append(" auto_increment");
        }
        builder.append(",");
    }

    private void getDefinitionOfDate(StringBuilder builder,Column column){
        builder.append(" datetime");
        if(column.nonnull()){
            builder.append(" not null");
        }
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(column.defaultValue());
        }
    }

    private void getDefinitionOfBoolean(StringBuilder builder,Column column){
        builder.append(" bit(1) ");
        if(column.nonnull()){
            builder.append(" not null");
        }
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(("true".equals(column.defaultValue()) ||
                    "1".equals(column.defaultValue())) ? 1 : 0);
        }
    }

    private void getDefinitionOfLong(StringBuilder builder,Column column){
        builder.append(" bigint(").append(column.length()).append(")");
        if(column.unsigned()){
            builder.append(" unsigned");
        }
        if(column.nonnull()){
            builder.append(" not null");
        }
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(column.defaultValue());
        }
    }

    private void getDefinitionOfShort(StringBuilder builder,Column column){
        builder.append(" tinyint(").append(column.length()).append(")");
        if(column.unsigned()){
            builder.append(" unsigned");
        }
        if(column.nonnull()){
            builder.append(" not null");
        }
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(column.defaultValue());
        }
    }

    private void getDefinitionOfInteger(StringBuilder builder,Column column){
        builder.append(column.length() < 65535 ? " int(" : " mediumint(").append(column.length())
                .append(")");
        if(column.unsigned()){
            builder.append(" unsigned");
        }
        if(column.nonnull()){
            builder.append(" not null");
        }
        if(!column.defaultValue().isEmpty()){
            builder.append(" default ").append(column.defaultValue());
        }
    }

    private void getDefinitionOfString(StringBuilder builder,Column column){
        builder.append(" varchar(").append(column.length()).append(")");
        if(column.nonnull()){
            builder.append(" not null");
        }
        if(!column.defaultValue().isEmpty()){
            builder.append(" default '").append(column.defaultValue()).append("'");
        }
    }

    private void getColumns(Class<?> clazz) {
        Set<Field> fields = Arrays.stream(clazz.getDeclaredFields()).filter(e -> e.isAnnotationPresent(Column.class))
                .collect(Collectors.toSet());
        List<Field> ids = fields.stream().filter(e -> e.isAnnotationPresent(Id.class)).collect(Collectors.toList());

        int size = ids.size();
        boolean idExist = true;
        if(size > 1){
            throw new ExceptionWrapper(ExceptionEnum.ID_DUPLICATED);
        }else if(size == 0){
            idExist = false;
            ids = fields.stream().filter(e -> e.isAnnotationPresent(PrimaryKey.class)).collect(Collectors.toList());
            size = ids.size();
            if(size == 0){
                throw new ExceptionWrapper(ExceptionEnum.ID_NULL);
            }
            if(size > 1){
                throw new ExceptionWrapper(ExceptionEnum.ID_DUPLICATED);
            }
        }

        List<Field> properties = new ArrayList<>();
        properties.add(ids.get(0));
        boolean finalIdExist = idExist;
        properties.addAll(fields.stream().filter(e -> finalIdExist ? !e.isAnnotationPresent(Id.class) :
                !e.isAnnotationPresent(PrimaryKey.class)).collect(Collectors.toList()));

        tableMap = getTableMap();
        tableMap.put(clazz,properties);
    }

    public String getTableName(Class<?> clazz) {
        String tableName = clazz.getAnnotation(Table.class).table();
        return tableName.isEmpty() ? mapCamelCaseToUnderscore(clazz.getSimpleName()) : tableName;
    }

    public String getColumnName(Field e) {
        String columnName = e.getAnnotation(Column.class).column();
        return columnName.isEmpty() ? mapCamelCaseToUnderscore(e.getName()) : columnName;
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

}

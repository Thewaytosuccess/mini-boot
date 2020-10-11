package com.mvc.entity.method;

/**
 * @author xhzy
 */
public class Param {

    private Class<?> type;

    private String name;

    private Object value;

    public Param(Class<?> type, String name) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return "Param{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", value=" + value +
                '}';
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}

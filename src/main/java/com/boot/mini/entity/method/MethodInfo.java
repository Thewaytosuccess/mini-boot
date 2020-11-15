package com.boot.mini.entity.method;

import java.util.List;

/**
 * @author xhzy
 */
public class MethodInfo extends Signature{

    private int modifiers;

    private Class<?> returnType;

    private List<Param> params;

    public MethodInfo(String methodName,List<Param> params){
        this.setMethodName(methodName);
        this.params = params;
    }

    public static String getter(String columnName){
        String getter = "get" + columnName.substring(0,1).toUpperCase();
        if(columnName.length() > 1){
            getter += columnName.substring(1);
        }
        return getter;
    }

    public static String setter(String name){
        String setter = "set" + name.substring(0, 1).toUpperCase();
        if(name.length() > 1){
            setter += name.substring(1);
        }
        return setter;
    }

    public int getModifiers() {
        return modifiers;
    }

    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public List<Param> getParams() {
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "methodName=" + getMethodName() +
                '}';
    }
}

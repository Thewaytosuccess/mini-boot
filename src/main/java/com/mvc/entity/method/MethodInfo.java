package com.mvc.entity.method;

import com.mvc.enums.AdviceEnum;

import java.util.List;

/**
 * @author xhzy
 */
public class MethodInfo extends Signature{

    private int modifiers;

    private Class<?> returnType;

    private boolean compared = false;

    private List<Param> params;

    public MethodInfo(){

    }

    public MethodInfo(String methodName,List<Param> params){
        this.setMethodName(methodName);
        this.params = params;
    }

    public MethodInfo(int parameterCount, Class<?>[] parameterTypes, String methodName, AdviceEnum adviceEnum, String adviceMethod) {
        super(parameterCount, parameterTypes, methodName, adviceEnum, adviceMethod);
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

    public boolean isCompared() {
        return compared;
    }

    public void setCompared(boolean compared) {
        this.compared = compared;
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

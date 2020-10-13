package com.mvc.entity.method;

import com.mvc.enums.AdviceEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author xhzy
 */
public class MethodInfo {

    /**
     * 方法上的切面类型
     */
    AdviceEnum adviceEnum;

    /**
     * 方法对应的切面方法
     */
    private String adviceMethod;

    private int modifiers;

    private Class<?> returnType;

    private String methodName;

    private int parameterCount;

    private Class<?>[] parameterTypes;

    private boolean compared = false;

    private List<Param> params;

    public MethodInfo(){

    }

    public MethodInfo(String methodName, List<Param> params) {
        this.methodName = methodName;
        this.params = params;
    }

    public String getAdviceMethod() {
        return adviceMethod;
    }

    public void setAdviceMethod(String adviceMethod) {
        this.adviceMethod = adviceMethod;
    }

    public AdviceEnum getAdviceEnum() {
        return adviceEnum;
    }

    public void setAdviceEnum(AdviceEnum adviceEnum) {
        this.adviceEnum = adviceEnum;
    }

    public void setCompared(boolean compared) {
        this.compared = compared;
    }

    public boolean isCompared() {
        return compared;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public void setParameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    public int getParameterCount() {
        return parameterCount;
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

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public List<Param> getParams() {
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodInfo info = (MethodInfo) o;
        if(compared){
            return Objects.equals(methodName, info.methodName) &&
                    Objects.equals(parameterCount, info.parameterCount) &&
                    Arrays.equals(parameterTypes, info.getParameterTypes());
        }else{
            return Objects.equals(methodName, info.methodName);
        }

    }

    @Override
    public int hashCode() {
        if(compared){
            return Objects.hash(methodName, parameterCount,parameterTypes);
        }else{
            return Objects.hash(methodName);
        }
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "methodName='" + methodName + '\'' +
                '}';
    }
}

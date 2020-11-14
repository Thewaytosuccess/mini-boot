package com.boot.mini.entity.method;

import com.boot.mini.enums.AdviceEnum;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author xhzy
 */
public class Signature {

    private int parameterCount;

    private Class<?>[] parameterTypes;

    private String methodName;

    /**
     * 方法上的切面类型
     */
    AdviceEnum adviceEnum;

    /**
     * 方法对应的切面方法
     */
    private String adviceMethod;

    public Signature(){

    }

    public Signature(int parameterCount, Class<?>[] parameterTypes, String methodName){
        this.parameterCount = parameterCount;
        this.parameterTypes = parameterTypes;
        this.methodName = methodName;
    }

    public Signature(int parameterCount, Class<?>[] parameterTypes, String methodName, AdviceEnum adviceEnum, String adviceMethod) {
        this.parameterCount = parameterCount;
        this.parameterTypes = parameterTypes;
        this.methodName = methodName;
        this.adviceEnum = adviceEnum;
        this.adviceMethod = adviceMethod;
    }

    public AdviceEnum getAdviceEnum() {
        return adviceEnum;
    }

    public void setAdviceEnum(AdviceEnum adviceEnum) {
        this.adviceEnum = adviceEnum;
    }

    public String getAdviceMethod() {
        return adviceMethod;
    }

    public void setAdviceMethod(String adviceMethod) {
        this.adviceMethod = adviceMethod;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public void setParameterCount(int parameterCount) {
        this.parameterCount = parameterCount;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Signature signature = (Signature) o;
        return parameterCount == signature.parameterCount &&
                Arrays.equals(parameterTypes, signature.parameterTypes) &&
                Objects.equals(methodName, signature.methodName);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(parameterCount, methodName);
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    @Override
    public String toString() {
        return "Signature{" +
                "parameterCount=" + parameterCount +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                ", methodName='" + methodName + '\'' +
                ", adviceEnum=" + adviceEnum +
                ", adviceMethod='" + adviceMethod + '\'' +
                '}';
    }
}

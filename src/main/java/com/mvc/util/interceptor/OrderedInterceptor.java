package com.mvc.util.interceptor;

import java.io.Serializable;

/**
 * @author xhzy
 */
public class OrderedInterceptor implements Comparable<OrderedInterceptor>, Serializable {

    private int order;

    private Class<?> clazz;

    public OrderedInterceptor(Class<?> clazz, int order) {
        this.clazz = clazz;
        this.order = order;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public int getOrder() {
        return order;
    }

    public void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int compareTo(OrderedInterceptor o) {
        return o.order - this.order;
    }
}

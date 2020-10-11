package com.mvc.base;

import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.regex.Pattern;

public class BaseTest {

    public static void main(String[] args) {
        System.out.println(Modifier.PUBLIC);
    }

    public void testProxy() throws Exception {
        Audi audi = new Audi();
        Object instance = Proxy.newProxyInstance(audi.getClass().getClassLoader(), audi.getClass().getInterfaces(),
                new CarProxy(audi));
        instance.getClass().getDeclaredMethod("run").invoke(instance);
    }

    public void testRegExp(){
        String reg = "^/api/user/delete/\\w+/\\w+";
        String uri = "/api/user/delete/jack/tom";
        System.out.println(Pattern.matches(reg,uri));
    }
}

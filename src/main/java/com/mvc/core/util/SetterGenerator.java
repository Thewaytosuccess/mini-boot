package com.mvc.core.util;

import com.mvc.core.exception.ExceptionWrapper;
import javassist.*;
import javassist.util.proxy.ProxyFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态生成setter方法
 * @author xhzy
 */
public class SetterGenerator {

    private static final Map<Class<?>,Object> PROXY_MAP = new ConcurrentHashMap<>();

    /**
     * 生成setter/constructor
     * @param clazz 当前类
     * @param fields 需要setter的字段
     */
    public static void generate(Class<?> clazz, List<Field> fields, boolean bySetter){
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(clazz));
        try {
            CtClass ctClass = getProxy(pool, clazz);
            if(bySetter){
                //生成setter
               fields.forEach(e -> generateSetter(ctClass,e,pool));
            }else{
                //生成constructor
               generateConstructor(ctClass,fields,pool);
            }

            ctClass.setSuperclass(pool.get(clazz.getName()));
            //调用writeFile/toClass方法后，ctClass就不允许修改了
            ctClass.writeFile("target/classes/");
            PROXY_MAP.put(clazz,ctClass.toClass().newInstance());
        } catch (Exception e) {
            throw new ExceptionWrapper(e);
        }
    }

    private static void generateConstructor(CtClass ctClass,List<Field> fields,ClassPool pool) throws Exception{
        int i = 0;
        CtClass[] parameters = new CtClass[fields.size()];
        StringBuilder builder = new StringBuilder("{");
        for(Field f:fields){
            parameters[i++] = pool.get(f.getType().getName());
            //$0 = this,$i代表第i个参数
            builder.append("$0.").append(getSimpleFieldName(f)).append("=$").append(i).append(";");
        }
        builder.append("}");
        //生成构造方法
        CtConstructor constructor = new CtConstructor(parameters,ctClass);
        constructor.setBody(builder.toString());
        ctClass.addConstructor(constructor);
    }

    private static void generateSetter(CtClass ctClass,Field f,ClassPool pool){
        try{
            //生成字段
            CtField ctField= new CtField(pool.get(f.getType().getName()),getSimpleFieldName(f),ctClass);
            ctField.setModifiers(Modifier.PRIVATE);
            ctClass.addField(ctField);
            //生成字段的setter方法
            ctClass.addMethod(CtNewMethod.setter("set"+f.getType().getSimpleName(),ctField));
        }catch (Exception e){
            throw new ExceptionWrapper(e);
        }
    }

    private static CtClass getProxy(ClassPool pool,Class<?> clazz) {
        //先判断字节码是否存在，存在，则追加
        String proxyClass = clazz.getName()+"Proxy";
        //CtClass ctClass = pool.get(proxyClass);
        CtClass ctClass = pool.makeClass(proxyClass);
        if(Objects.isNull(ctClass)){
            //不存在，则创建
            ctClass = pool.makeClass(proxyClass);
        }
        if(ctClass.isFrozen()){
            ctClass.defrost();
        }
        return ctClass;
    }

    private static String getSimpleFieldName(Field f){
        String simpleName = f.getType().getSimpleName();
        String name = simpleName.substring(0,1).toLowerCase();
        if(simpleName.length() > 1){
            name += simpleName.substring(1);
        }
        return name;
    }

    static class CacheHelper extends ClassPool{
        //需要重启，没有实现热加载
        @Override
        public CtClass removeCached(String className) {
            return super.removeCached(className);
        }
    }

    /**
     * javassist 实现动态代理
     * @param clazz 需要被代理的类
     * @param methodName 需要拦截的方法
     */
    public static void dynamicProxy(Class<?> clazz,String methodName){
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(clazz);
        factory.setFilter(method -> method.getName().equals(methodName));
        factory.setHandler((o, method, proxy, args) -> {
            //method before
            return proxy.invoke(o,args);
        });
    }


}

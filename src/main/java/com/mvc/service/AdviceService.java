package com.mvc.service;

import com.mvc.annotation.aop.advice.*;
import com.mvc.annotation.aop.aspect.Aspect;
import com.mvc.annotation.config.Configuration;
import com.mvc.util.proxy.ProceedingJoinPoint;

/**
 * @author xhzy
 */
@Configuration
@Aspect
public class AdviceService {

//    @Around("@annotation(com.mvc.annotation.test.AccessGranted)")
//    public Object testBefore(ProceedingJoinPoint point){
//        System.out.println("before service");
//        Object proceed = point.proceed();
//        System.out.println("result = "+ proceed);
//        System.out.println("after service");
//        return proceed;
//    }

//    @Before("public com.mvc.controller.UserController.logout(..)")
//    public void testBefore(){
//        System.out.println("this is a test for before advice, ha ha=====");
//    }
//
//    @After("public com.mvc.controller.UserController.logout(..)")
//    public void testAfter(){
//        System.out.println("this is a test for after advice, hello world =====");
//    }

//    @Around("execution(public com.mvc.controller.*Controller.get*(..))")
//    public Object testAround(ProceedingJoinPoint point){
//        System.out.println("this is a test for [before advice] =====");
//        Object result = point.proceed();
//        System.out.println("result ===="+ result);
//        System.out.println("this is a test for [after advice] =====");
//        return result;
//    }

//    @AfterReturning(execution = "public com.mvc.service.impl.UserServiceImpl.getDataSourceConfig(..)")
//    public void testAfter(){
//        System.out.println("this is a test for [after returning] advice =====");
//    }

//    @AfterThrowing(execution = "public com.mvc.service.impl.UserServiceImpl.getDataSourceConfig(..)")
//    public void testAfter(){
//        System.out.println("this is a test for [after throwing] advice =====");
//    }


}

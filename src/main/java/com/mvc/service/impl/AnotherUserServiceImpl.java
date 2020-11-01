package com.mvc.service.impl;

import com.mvc.annotation.bean.ioc.Autowired;
import com.mvc.annotation.method.async.Async;
import com.mvc.annotation.type.service.Service;
import com.mvc.entity.test.DataSourceConfig;
import com.mvc.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author xhzy
 */
@Service
public class AnotherUserServiceImpl implements UserService {

    @Autowired
    private DataSourceConfig dataSourceConfig;

    @PostConstruct
    public void init(){
        System.out.println("AnotherUserServiceImpl 完成初始化。。。");
    }

    @PreDestroy
    public void destroy(){
        System.out.println("AnotherUserServiceImpl 完成销毁。。。");
    }

    @Async
    @Override
    public DataSourceConfig getDataSourceConfig(){
        return dataSourceConfig;
    }

}

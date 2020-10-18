package com.mvc.service.impl;

import com.mvc.annotation.bean.Autowired;
import com.mvc.annotation.bean.PostConstruct;
import com.mvc.annotation.bean.PreDestroy;
import com.mvc.annotation.type.service.Service;
import com.mvc.entity.test.DataSourceConfig;
import com.mvc.service.UserService;

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

    @Override
    public DataSourceConfig getDataSourceConfig(){
        return dataSourceConfig;
    }

}

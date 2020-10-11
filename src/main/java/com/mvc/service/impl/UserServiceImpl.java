package com.mvc.service.impl;

import com.mvc.annotation.bean.Autowired;
import com.mvc.annotation.type.Service;
import com.mvc.entity.test.DataSourceConfig;
import com.mvc.service.UserService;

/**
 * @author xhzy
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private DataSourceConfig dataSourceConfig;

    @Override
    public DataSourceConfig getDataSourceConfig(){
        return dataSourceConfig;
    }
}

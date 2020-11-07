package com.mvc.service.impl;

import com.mvc.annotation.bean.ioc.Autowired;
import com.mvc.annotation.test.AccessGranted;
import com.mvc.annotation.type.service.Service;
import com.mvc.entity.test.DataSourceConfig;
import com.mvc.entity.test.User;
import com.mvc.service.UserService;

import java.util.List;

/**
 * @author xhzy
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private DataSourceConfig dataSourceConfig;

    @Override
    @AccessGranted
    public DataSourceConfig getDataSourceConfig(){
        return dataSourceConfig;
    }

    @Override
    public boolean save(User user) {
        return false;
    }

    @Override
    public boolean delete(User user) {
        return false;
    }

    @Override
    public boolean update(User user) {
        return false;
    }

    @Override
    public List<User> get(User user) {
        return null;
    }
}

package com.boot.mini.service.impl;

import com.boot.mini.annotation.bean.ioc.Autowired;
import com.boot.mini.entity.test.DataSourceConfig;
import com.boot.mini.annotation.test.AccessGranted;
import com.boot.mini.annotation.type.service.Service;
import com.boot.mini.entity.test.User;
import com.boot.mini.service.UserService;

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
    public boolean delete(Long userId) {
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

    @Override
    public User getByUserId(Long userId) {
        return null;
    }
}

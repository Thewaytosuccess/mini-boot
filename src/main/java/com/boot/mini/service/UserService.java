package com.boot.mini.service;

import com.boot.mini.entity.test.DataSourceConfig;
import com.boot.mini.entity.test.User;

import java.util.List;

/**
 * @author xhzy
 */
public interface UserService {

    /**
     * test get config
     * @return config
     */
    DataSourceConfig getDataSourceConfig();

    boolean save(User user);

    boolean delete(Long userId);

    boolean update(User user);

    List<User> get(User user);



}

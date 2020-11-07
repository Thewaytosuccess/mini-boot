package com.mvc.service;

import com.mvc.entity.test.DataSourceConfig;
import com.mvc.entity.test.User;

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

    boolean delete(User user);

    boolean update(User user);

    List<User> get(User user);



}

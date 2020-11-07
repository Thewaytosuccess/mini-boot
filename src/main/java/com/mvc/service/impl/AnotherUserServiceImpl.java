package com.mvc.service.impl;

import com.mvc.annotation.bean.ioc.Autowired;
import com.mvc.annotation.method.async.Async;
import com.mvc.annotation.type.service.Service;
import com.mvc.entity.test.DataSourceConfig;
import com.mvc.entity.test.User;
import com.mvc.repository.UserRepository;
import com.mvc.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

/**
 * @author xhzy
 */
@Service
public class AnotherUserServiceImpl implements UserService {

    @Autowired
    private DataSourceConfig dataSourceConfig;

    @Autowired
    private UserRepository userRepository;

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

    @Override
    public boolean save(User user) {
        return userRepository.insert(user);
    }

    @Override
    public boolean delete(User user) {
        return userRepository.deleteByPrimaryKey(user);
    }

    @Override
    public boolean update(User user) {
        return userRepository.updateByPrimaryKey(user);
    }

    @Override
    public List<User> get(User user) {
        return userRepository.select(user);
    }

}

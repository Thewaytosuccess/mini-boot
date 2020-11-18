package com.boot.mini.service.impl;

import com.boot.mini.annotation.bean.ioc.Autowired;
import com.boot.mini.core.datasource.wrapper.impl.QueryWrapper;
import com.boot.mini.entity.test.DataSourceConfig;
import com.boot.mini.annotation.method.async.Async;
import com.boot.mini.annotation.type.service.Service;
import com.boot.mini.entity.test.User;
import com.boot.mini.repository.UserRepository;
import com.boot.mini.service.UserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
        Date gmtCreated = user.getGmtCreated();
        if(Objects.isNull(gmtCreated)){
            user.setGmtCreated(new Date());
        }
        return userRepository.insert(user);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean delete(Long userId) {
        return userRepository.deleteByPrimaryKey(userId);
    }

    @Override
    public boolean update(User user) {
        return userRepository.updateByPrimaryKey(user);
    }

    @Override
    public List<User> get(User user) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>(){};
        if(Objects.nonNull(user)){
            Long userId = user.getUserId();
            if(Objects.nonNull(userId)){
                wrapper.eq("user_id", userId);
            }
            String userName = user.getUserName();
            if(Objects.nonNull(userName)){
                wrapper.like("user_name",userName);
            }

            wrapper.orderByDesc("gmt_created");
            wrapper.limit(1,1);
        }

        return userRepository.select(wrapper);
    }

    @Override
    public User getByUserId(Long userId) {
        return userRepository.selectByPrimaryKey(userId);
    }

}

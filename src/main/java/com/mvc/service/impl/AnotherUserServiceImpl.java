package com.mvc.service.impl;

import com.mvc.annotation.bean.ioc.Autowired;
import com.mvc.annotation.method.async.Async;
import com.mvc.annotation.type.service.Service;
import com.mvc.core.datasource.wrapper.impl.QueryWrapper;
import com.mvc.entity.test.DataSourceConfig;
import com.mvc.entity.test.User;
import com.mvc.repository.UserRepository;
import com.mvc.service.UserService;

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
        return userRepository.select(wrapper);
    }

}

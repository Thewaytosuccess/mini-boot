package com.mvc.controller;

import com.mvc.annotation.bean.ioc.Resource;
import com.mvc.annotation.config.Value;
import com.mvc.annotation.method.http.DeleteMapping;
import com.mvc.annotation.method.http.GetMapping;
import com.mvc.annotation.method.http.PostMapping;
import com.mvc.annotation.method.http.RequestMapping;
import com.mvc.annotation.param.PathVariable;
import com.mvc.annotation.param.RequestBody;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.core.exception.ExceptionWrapper;
import com.mvc.entity.test.ScheduleParam;
import com.mvc.enums.ExceptionEnum;
import com.mvc.service.UserService;

/**
 * @author xhzy
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${spring.dataSource.user-name}")
    private String username;

    @Resource(name="anotherUserServiceImpl")
    private UserService userService;

    @GetMapping("/get")
    public Object getUser(){
        throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
    }

    @PostMapping("/login")
    public Object login(@RequestBody ScheduleParam user){
        return "post:"+user;
    }

    @DeleteMapping("/delete/{userId}/user")
    public Object delete(@PathVariable Long userId){
        return "delete:userId="+userId;
    }

    @RequestMapping("/logout")
    public Object logout(){
        return userService.getDataSourceConfig();
    }
}

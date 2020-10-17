package com.mvc.controller;

import com.mvc.annotation.bean.Resource;
import com.mvc.annotation.config.Value;
import com.mvc.annotation.method.DeleteMapping;
import com.mvc.annotation.method.GetMapping;
import com.mvc.annotation.method.PostMapping;
import com.mvc.annotation.method.RequestMapping;
import com.mvc.annotation.param.PathVariable;
import com.mvc.annotation.param.RequestBody;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.entity.test.User;
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
        return "get:username="+username;
    }

    @PostMapping("/login")
    public Object login(@RequestBody User user){
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
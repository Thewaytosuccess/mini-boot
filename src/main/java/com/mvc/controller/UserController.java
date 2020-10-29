package com.mvc.controller;

import com.mvc.annotation.bean.ioc.Autowired;
import com.mvc.annotation.bean.ioc.Qualifier;
import com.mvc.annotation.config.Value;
import com.mvc.annotation.method.http.DeleteMapping;
import com.mvc.annotation.method.http.GetMapping;
import com.mvc.annotation.method.http.PostMapping;
import com.mvc.annotation.method.http.RequestMapping;
import com.mvc.annotation.param.PathVariable;
import com.mvc.annotation.param.RequestBody;
import com.mvc.annotation.type.controller.RestController;
import com.mvc.entity.test.User;
import com.mvc.service.UserService;

/**
 * @author xhzy
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Value("${spring.dataSource.user-name}")
    private String username;

    @Autowired
    @Qualifier(name = "userServiceImpl")
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

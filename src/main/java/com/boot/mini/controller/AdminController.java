package com.boot.mini.controller;

import com.boot.mini.annotation.bean.ioc.Resource;
import com.boot.mini.annotation.method.http.*;
import com.boot.mini.annotation.param.PathVariable;
import com.boot.mini.annotation.param.RequestBody;
import com.boot.mini.annotation.type.controller.RestController;
import com.boot.mini.entity.test.User;
import com.boot.mini.service.UserService;

/**
 * @author xhzy
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource(name="anotherUserServiceImpl")
    private UserService userService;

    @GetMapping("/get")
    public Object getUser(User user){
        return userService.get(user);
    }

    @GetMapping("/get/{userId}")
    public Object getByUserId(@PathVariable("userId") Long userId){
        return userService.getByUserId(userId);
    }

    @PostMapping("/update")
    public Object update(@RequestBody User user){
        return userService.update(user);
    }

    @DeleteMapping("/delete/{userId}")
    public Object delete(@PathVariable Long userId){
        return userService.delete(userId);
    }

    @PutMapping("/save")
    public Object save(@RequestBody User user){
        return userService.save(user);
    }
}

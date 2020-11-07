package com.mvc.controller;

import com.mvc.annotation.bean.ioc.Resource;
import com.mvc.annotation.method.http.*;
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

    @Resource(name="anotherUserServiceImpl")
    private UserService userService;

    @GetMapping("/get/{userId}")
    public Object getUser(@PathVariable Long userId){
        User user = new User();
        user.setUserId(userId);
        return userService.get(user);
    }

    @PostMapping("/update")
    public Object update(@RequestBody User user){
        return userService.update(user);
    }

    @DeleteMapping("/delete/{userId}")
    public Object delete(@PathVariable Long userId){
        User user = new User();
        user.setUserId(userId);
        return userService.delete(user);
    }

    @PutMapping("/save")
    public Object save(@RequestBody User user){
        return userService.save(user);
    }
}

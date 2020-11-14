package com.boot.mini.controller;

import com.boot.mini.annotation.bean.ioc.Autowired;
import com.boot.mini.annotation.bean.ioc.Qualifier;
import com.boot.mini.annotation.method.http.DeleteMapping;
import com.boot.mini.annotation.type.controller.RestController;
import com.boot.mini.entity.test.ScheduleParam;
import com.boot.mini.annotation.method.http.GetMapping;
import com.boot.mini.annotation.method.http.PostMapping;
import com.boot.mini.annotation.method.http.RequestMapping;
import com.boot.mini.annotation.param.PathVariable;
import com.boot.mini.annotation.param.RequestBody;
import com.boot.mini.core.task.schedule.job.ScheduledJobManager;
import com.boot.mini.service.UserService;

/**
 * @author xhzy
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    @Qualifier(name = "userServiceImpl")
    private UserService userService;

    @GetMapping("/job/close/{jobId}")
    public Object closeJob(@PathVariable("jobId") Integer jobId){
        return ScheduledJobManager.getInstance().deleteJob(jobId);
    }

    @PostMapping("/reschedule")
    public Object reschedule(@RequestBody ScheduleParam param){
        ScheduledJobManager.getInstance().rescheduleJob(param.getJobId(),param.getCron());
        return null;
    }

    @DeleteMapping("/delete/{userId}/user")
    public Object delete(@PathVariable Long userId){
        return "delete:userId="+userId;
    }

    @RequestMapping("/logout")
    public Object reschedule(){
        return userService.getDataSourceConfig();
    }
}

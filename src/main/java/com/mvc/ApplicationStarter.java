package com.mvc;

import com.mvc.annotation.enable.EnableScheduling;
import com.mvc.annotation.method.schedule.Scheduled;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.annotation.type.component.ComponentScan;

/**
 * @author xhzy
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.mvc")
//@EnableAsync
@EnableScheduling
//@EnableAspectJAutoProxy
public class ApplicationStarter {

    @Scheduled(prefix = "quartz.job")
    public void testScheduled(){
        System.out.println("【scheduled task is running】");
    }


}

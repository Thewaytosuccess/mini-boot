package com.mvc;

import com.mvc.annotation.enable.EnableDataSourceAutoConfiguration;
import com.mvc.annotation.method.schedule.Scheduled;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.annotation.type.component.ComponentScan;

/**
 * @author xhzy
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.mvc")
@EnableDataSourceAutoConfiguration
public class ApplicationStarter {

    @Scheduled(prefix = "quartz.job")
    public void testScheduled(){
        System.out.println("【scheduled task is running】");
    }


}

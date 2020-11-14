package com.boot.mini;

import com.boot.mini.annotation.method.schedule.Scheduled;
import com.boot.mini.annotation.type.SpringBootApplication;
import com.boot.mini.annotation.type.component.ComponentScan;
import com.boot.mini.annotation.enable.EnableDataSourceAutoConfiguration;

/**
 * @author xhzy
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.boot.mini")
@EnableDataSourceAutoConfiguration
public class ApplicationStarter {

    @Scheduled(prefix = "quartz.job")
    public void testScheduled(){
        System.out.println("【scheduled task is running】");
    }


}

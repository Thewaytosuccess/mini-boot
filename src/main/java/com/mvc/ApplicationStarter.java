package com.mvc;

import com.mvc.annotation.enable.EnableAsync;
import com.mvc.annotation.type.SpringBootApplication;
import com.mvc.annotation.type.component.ComponentScan;

/**
 * @author xhzy
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.mvc")
@EnableAsync
//@EnableAspectJAutoProxy
public class ApplicationStarter {


}

package com.mvc.annotation.bean.ioc;

import java.lang.annotation.*;

/**
 * @author xhzy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
@Inherited
public @interface Autowired {

}

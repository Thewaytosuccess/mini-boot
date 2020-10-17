package com.mvc.util.exception;

/**
 * @author xhzy
 */
public interface ExceptionHandler {

    /**
     * 异常处理方法
     * @param e 异常
     * @return 处理结果
     */
    Object handle(Exception e);

}

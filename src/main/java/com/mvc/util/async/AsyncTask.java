package com.mvc.util.async;

import com.mvc.annotation.bean.life.Async;
import com.mvc.util.injection.IocContainer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xhzy
 */
public class AsyncTask {

    public void doAsync(){
        List<Method> tasks = new ArrayList<>();
        IocContainer.getInstance().getClasses().forEach(e -> tasks.addAll(Arrays.stream(e.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Async.class)).collect(Collectors.toList())));

    }
}

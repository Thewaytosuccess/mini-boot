package com.mvc.util.invocation;

import com.alibaba.fastjson.JSON;
import com.mvc.entity.method.MethodInfo;
import com.mvc.entity.method.Param;
import com.mvc.util.binding.DataBindingProcessor;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.DependencyInjectProcessor;
import com.mvc.util.mapping.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.mvc.enums.constant.ConstantPool.PATH_SEPARATOR;

/**
 * @author xhzy
 */
public class InvocationProcessor {

    private static final InvocationProcessor PROCESSOR = new InvocationProcessor();

    private InvocationProcessor(){}

    public static InvocationProcessor getInstance(){ return PROCESSOR; }

    public Object process(HttpServletRequest request) {
        MethodInfo methodInfo = HandlerMapping.getInstance().getMethodInfo(request.getRequestURI(),
                request.getMethod());
        System.out.println("METHOD === "+methodInfo);

        DataBindingProcessor dataBindingProcessor = DataBindingProcessor.getInstance();
        if(Objects.nonNull(methodInfo)){
            List<Param> params = methodInfo.getParams();
            Map<String, String[]> parameterMap = request.getParameterMap();
            if(!parameterMap.isEmpty()){
                params.forEach(e -> {
                    String[] values = parameterMap.get(e.getName());
                    if(Objects.nonNull(values) && values.length > 0){
                        //前端参数能够和后端接口参数一一映射，则建立映射
                        dataBindingProcessor.setValue(values,e);
                    }else{
                        //否则，将前端所有参数映射成接口需要的一个对象
                        dataBindingProcessor.setValue(parameterMap,e);
                    }
                });
            }else{
                String requestJson = dataBindingProcessor.getRequestJson(request);
                if(Objects.nonNull(requestJson) && !requestJson.isEmpty()){
                    Param param = params.get(0);
                    if(Objects.nonNull(param)){
                        param.setValue(JSON.parseObject(requestJson, param.getType()));
                    }
                }
            }
            return invoke(methodInfo);
        }
        return null;
    }

    private Object invoke(MethodInfo methodInfo){
        if(Objects.nonNull(methodInfo)){
            String methodName = methodInfo.getMethodName();
            List<Param> params = methodInfo.getParams();
            int index = methodName.lastIndexOf(PATH_SEPARATOR);
            try {
                Class<?> clazz = Class.forName(methodName.substring(0, index));
                //从ioc容器中查询实例
                return clazz.getDeclaredMethod(methodName.substring(index + 1), params.stream().map(Param::getType).toArray(Class[]::new))
                        .invoke(DependencyInjectProcessor.getInstance().getClassInstance(clazz),
                                params.stream().map(Param::getValue).toArray(Object[]::new));
            } catch (Exception e) {
                throw new ExceptionWrapper(e);
            }
        }
        return null;
    }

}

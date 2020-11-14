package com.boot.mini.core.mapping;

import com.boot.mini.annotation.method.http.*;
import com.boot.mini.annotation.param.RequestParam;
import com.boot.mini.entity.method.MethodInfo;
import com.boot.mini.entity.method.Param;
import com.mvc.annotation.method.http.*;
import com.boot.mini.annotation.param.PathVariable;
import com.boot.mini.annotation.param.RequestBody;
import com.boot.mini.enums.HttpMethodEnum;
import com.boot.mini.core.binding.DataBindingProcessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.boot.mini.enums.constant.ConstantPool.*;

/**
 * @author xhzy
 */
public class HandlerMapping {

    private static final HandlerMapping HANDLER_MAPPING = new HandlerMapping();

    private HandlerMapping(){}

    public static HandlerMapping getInstance(){
        return HANDLER_MAPPING;
    }

    private Map<String, MethodInfo> getMap;

    private Map<String, MethodInfo> postMap;

    private Map<String, MethodInfo> putMap;

    private Map<String, MethodInfo> deleteMap;

    private Map<String, MethodInfo> requestMap;

    private String uriPrefix = "";

    /**
     * 解析@RequestMapping,@GetMapping,@PostMapping所注解的方法，拼接成url,
     * 统一注册到map<url,package.class.method>
     */
    public void buildMapping(){
        //待处理@Controller
        PackageScanner.getInstance().getControllers().forEach(e -> {
            //解析类上的@RequestMapping，拼装uri前缀
            getUriPrefix(e);
            //建立uri和对应的方法的映射
            methodMapping(e);
        });
    }

    public MethodInfo getMethodInfo(String uri,String method){
        MethodInfo methodInfo = null;
        //精确匹配
        DataBindingProcessor processor = DataBindingProcessor.getInstance();
        if(HttpMethodEnum.GET.getMethod().equalsIgnoreCase(method)){
            methodInfo = getMap.get(uri);
            //针对@PathVariable的模糊匹配
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(getMap,uri);
            }
        }else if(HttpMethodEnum.POST.getMethod().equalsIgnoreCase(method)){
            methodInfo = postMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(postMap,uri);
            }
        }else if(HttpMethodEnum.DELETE.getMethod().equalsIgnoreCase(method)){
            methodInfo = deleteMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(deleteMap,uri);
            }
        }else if(HttpMethodEnum.PUT.getMethod().equalsIgnoreCase(method)){
            methodInfo = putMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(putMap,uri);
            }
        }
        if(Objects.isNull(methodInfo)){
            methodInfo = requestMap.get(uri);
            if(Objects.isNull(methodInfo)){
                methodInfo = processor.patternMatch(requestMap,uri);
            }
        }
        return methodInfo;
    }

    private void getUriPrefix(Class<?> clazz){
        if(clazz.isAnnotationPresent(RequestMapping.class)){
            RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
            uriPrefix = requestMapping.value();
            if(!uriPrefix.isEmpty() && uriPrefix.endsWith(URL_SEPARATOR)){
                uriPrefix = uriPrefix.substring(0,uriPrefix.length()-1);
            }
        }
    }

    private String getUri(String uriSuffix){
        if(!uriSuffix.isEmpty() && !uriSuffix.startsWith(URL_SEPARATOR)){
            uriSuffix = URL_SEPARATOR + uriSuffix;
        }
        return uriPrefix + uriSuffix;
    }

    private List<Param> parseParameters(Method method){
        //参数的注解
        Parameter[] parameters = method.getParameters();
        List<Param> params = new ArrayList<>();
        for(Parameter p:parameters){
            if(p.isAnnotationPresent(PathVariable.class)){
                String name = p.getAnnotation(PathVariable.class).value();
                if(!name.isEmpty()){
                    params.add(new Param(p.getType(),name));
                }else{
                    params.add(new Param(p.getType(),p.getName()));
                }
            }else if(p.isAnnotationPresent(RequestParam.class)){
                String name = p.getAnnotation(RequestParam.class).value();
                if(!name.isEmpty()){
                    params.add(new Param(p.getType(),name));
                }else{
                    params.add(new Param(p.getType(),p.getName()));
                }
            }else if(p.isAnnotationPresent(RequestBody.class)){
                params.add(new Param(p.getType(),null));
            }else{
                //没有注解的也要保存参数名称
                params.add(new Param(p.getType(),p.getName()));
            }
        }
        return params;
    }

    /**
     * 方法上和方法参数上的注解解析
     * @param clazz 类的字节码对象
     */
    private void methodMapping(Class<?> clazz){
        //getDeclaredMethods可以获取到不包括继承的所有方法，而getMethods只能获取到公有方法
        Method[] methods = clazz.getDeclaredMethods();
        MethodInfo methodInfo;
        for(Method method:methods){
            methodInfo = new MethodInfo(clazz.getName() + PATH_SEPARATOR + method.getName(),
                    parseParameters(method));
            if(method.isAnnotationPresent(GetMapping.class)){
                getMap = initMap(getMap);
                getMap.put(getUri(method.getAnnotation(GetMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(PostMapping.class)){
                postMap = initMap(postMap);
                postMap.put(getUri(method.getAnnotation(PostMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(DeleteMapping.class)){
                deleteMap = initMap(deleteMap);
                deleteMap.put(getUri(method.getAnnotation(DeleteMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(PutMapping.class)){
                putMap = initMap(putMap);
                putMap.put(getUri(method.getAnnotation(PutMapping.class).value()), methodInfo);
            }else if(method.isAnnotationPresent(RequestMapping.class)){
                requestMap = initMap(requestMap);
                requestMap.put(getUri(method.getAnnotation(RequestMapping.class).value()), methodInfo);
            }
        }
    }

    private Map<String, MethodInfo> initMap(Map<String, MethodInfo> map){
        return Objects.isNull(map) ? new HashMap<>(16) : map;
    }

    public void print(){
        if(Objects.nonNull(getMap)){
            getMap.forEach((k, v) -> System.out.println("Mapping GET [" + k + ":" + v + "]"));
        }

        if(Objects.nonNull(postMap)){
            postMap.forEach((k, v) -> System.out.println("Mapping POST [" + k + ":" + v + "]"));
        }

        if(Objects.nonNull(deleteMap)){
            deleteMap.forEach((k, v) -> System.out.println("Mapping DELETE [" + k + ":" + v + "]"));
        }

        if(Objects.nonNull(putMap)){
            putMap.forEach((k, v) -> System.out.println("Mapping PUT ["+ k + ":" + v + "]"));
        }

        if(Objects.nonNull(requestMap)){
            requestMap.forEach((k, v) -> System.out.println("Mapping REQUEST ["+ k + ":" + v + "]"));
        }
    }

}

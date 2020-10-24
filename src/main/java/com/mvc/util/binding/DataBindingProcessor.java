package com.mvc.util.binding;

import com.alibaba.fastjson.JSONObject;
import com.mvc.entity.method.MethodInfo;
import com.mvc.entity.method.Param;
import com.mvc.util.exception.ExceptionWrapper;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.mvc.enums.constant.ConstantPool.URL_SEPARATOR;

/**
 * @author xhzy
 */
public class DataBindingProcessor {

    private static final DataBindingProcessor PROCESSOR = new DataBindingProcessor();

    private DataBindingProcessor(){}

    public static DataBindingProcessor getInstance(){
        return PROCESSOR;
    }

    /**
     * 正则匹配uri，并将uri中的参数解析出来，保存到methodInfo中
     * @param map 注解中的uri和对应的方法的映射
     * @param uri 前端请求uri
     * @return 注解对应的方法信息
     */
    public MethodInfo patternMatch(Map<String, MethodInfo> map, String uri) {
        if(!map.isEmpty()){
            List<String> list = map.keySet().stream().filter(e -> e.contains("{") && e.contains("}"))
                    .collect(Collectors.toList());
            if(!list.isEmpty()){
                //解析含有pathVariable的uri
                Map<String,String> patternMap = new HashMap<>(16);
                List<String> parameterNames = new ArrayList<>();
                List<String> separators = new ArrayList<>();
                list.forEach(e -> patternMap.put(toRegExp(e,parameterNames,separators),e));

                Map<String, String> parameterMap = getParameterMap(parameterNames, getValues(separators, uri));
                if(!parameterMap.isEmpty()){
                    Set<String> keySet = patternMap.keySet();
                    for(String regExp:keySet){
                        if(uri.matches(regExp)){
                            MethodInfo methodInfo = map.get(patternMap.get(regExp));
                            methodInfo.getParams().forEach(e -> setValue(new String[]{parameterMap.get(e.getName())},e));
                            return methodInfo;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 提取前端请求uri中绑定的pathVariable
     * @param separators 需要被替换掉的字符串
     * @param uri 前端请求uri
     * @return 提取的路径中的参数
     */
    private List<String> getValues(List<String> separators, String uri) {
        for(String s:separators){
            if(!s.equals(URL_SEPARATOR)){
                if(s.startsWith(URL_SEPARATOR) && s.endsWith(URL_SEPARATOR)){
                    s = s.substring(1);
                }
                uri = uri.replaceAll(s,"");
            }
        }
        return Arrays.stream(uri.split("/")).filter(e -> !e.isEmpty()).collect(Collectors.toList());
    }

    /**
     * 将后端注解中uri中的参数名和前端请求的uri中的参数值进行映射/绑定
     * @param names 参数名，来自于pathVariable
     * @param values 参数值，来自于前端uri
     * @return 参数名和参数值的映射
     */
    private Map<String,String> getParameterMap(List<String> names, List<String> values) {
        Map<String,String> map = new HashMap<>(16);
        for(int i=0,len = values.size();i < len ; ++i){
            map.put(names.get(i),values.get(i));
        }
        return map;
    }

    /**
     * 将注解中的uri转换成正则表达式，同时提取
     * @param uri 后端注解中的uri
     * @param names uri中提取的参数名
     * @param separators uri中提取的分隔符
     * @return 正则表达式
     */
    private String toRegExp(String uri, List<String> names, List<String> separators){
        StringBuilder sb = new StringBuilder("^");
        StringBuilder name = new StringBuilder();
        StringBuilder separator = new StringBuilder();

        char[] chars = uri.toCharArray();
        for(int i=0,len = chars.length;i < len;++i){
            while(i < len && chars[i] != '{'){
                separator.append(chars[i]);
                sb.append(chars[i++]);
            }
            if(separator.length() > 0){
                separators.add(separator.toString());
                //clear
                separator.delete(0,separator.length());
            }

            i++;
            while(i < len && chars[i] != '}'){
                name.append(chars[i]);
                ++i;
            }
            if(name.length() > 0){
                names.add(name.toString());
                //clear
                name.delete(0,name.length());
            }

            if(i < len){
                sb.append("\\w+");
            }
        }
        return sb.toString();
    }

    /**
     * 获取前端传递的json字符串
     * @param request request对象
     * @return json串
     */
    public String getRequestJson(HttpServletRequest request){
        try (ServletInputStream inputStream = request.getInputStream()){
            //json request
            if(Objects.nonNull(inputStream)){
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                StringBuilder builder = new StringBuilder();
                while((line = reader.readLine()) != null){
                    builder.append(line);
                }
                return builder.toString();
            }
        } catch (IOException e) {
            throw new ExceptionWrapper(e);
        }
        return null;
    }

    /**
     * 将前端请求参数转化成方法中需要的类型
     * @param array 参数值
     * @param e 方法中的参数
     */
    public void setValue(String[] array, Param e){
        Class<?> type = e.getType();
        if(Integer.class == type){
            e.setValue(Integer.valueOf(array[0]));
        }else if(Long.class == type){
            e.setValue(Long.valueOf(array[0]));
        }else if(String.class == type){
            e.setValue(array[0]);
        }else if(List.class == type){
            e.setValue(new ArrayList<>(Arrays.asList(array)));
        }else if(Set.class == type){
            e.setValue(new HashSet<>(new ArrayList<>(Arrays.asList(array))));
        }else if(type.isPrimitive()){
            //base type
            e.setValue(array[0]);
        }else if(type.isArray()){
            //array type
            e.setValue(array);
        }
    }

    /**
     * 将前端请求参数转化成方法中需要的类型
     * @param map 前端请求参数
     * @param p 方法参数
     */
    public void setValue(Map<String,String[]> map,Param p){
        Class<?> clazz = p.getType();
        Field[] declaredFields = clazz.getDeclaredFields();
        JSONObject json = new JSONObject();

        String[] array;
        Class<?> type;
        String name;
        for(Field f:declaredFields){
            name = f.getName();
            type = f.getType();
            array = map.get(name);
            if(Objects.nonNull(array) && array.length > 0){
                if(type == Integer.class){
                    json.put(name,Integer.valueOf(array[0]));
                }else if(type == Long.class){
                    json.put(name,Long.valueOf(array[0]));
                }else if(type == String.class){
                    json.put(name,array[0]);
                }else if(type == Short.class){
                    json.put(name,Short.valueOf(array[0]));
                }else if(type == Float.class){
                    json.put(name,Float.valueOf(array[0]));
                }else if(type == Character.class){
                    json.put(name, array[0].charAt(0));
                }else if(type == Boolean.class){
                    json.put(name,Boolean.valueOf(array[0]));
                }else if(f.getClass().isArray()){
                    json.put(name,array);
                }else if(f.getClass().isPrimitive()){
                    json.put(name,array[0]);
                }
            }
        }
        p.setValue(json.toJavaObject(p.getType()));
    }
}

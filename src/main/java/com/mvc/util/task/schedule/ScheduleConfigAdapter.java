package com.mvc.util.task.schedule;

import com.alibaba.fastjson.JSONObject;
import com.mvc.util.injection.ConfigurationProcessor;

import java.util.Map;
import java.util.Objects;

/**
 * @author xhzy
 */
public class ScheduleConfigAdapter {

    private ScheduleConfig config;

    /**
     * 配置定时任务
     */
    public void setConfig(ScheduleConfig config){
        if(Objects.isNull(config)){
            return;
        }

        String prefix = config.getPrefix();
        if(Objects.nonNull(prefix) && !prefix.isEmpty()){
            Map<String, Object> map = ConfigurationProcessor.getInstance().getByPrefix(prefix);
            if(!map.isEmpty()){
                this.config = JSONObject.parseObject(JSONObject.toJSONString(map),ScheduleConfig.class);
            }
        }else{
            this.config = config;
        }
    }

    private ScheduleConfig getConfig(){
        return config;
    }
}

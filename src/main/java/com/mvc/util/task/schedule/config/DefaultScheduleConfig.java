package com.mvc.util.task.schedule.config;

import com.alibaba.fastjson.JSONObject;
import com.mvc.enums.ExceptionEnum;
import com.mvc.util.exception.ExceptionWrapper;
import com.mvc.util.injection.ConfigurationProcessor;
import org.quartz.JobDataMap;

import java.util.Map;
import java.util.Objects;

/**
 * @author xhzy
 */
public class DefaultScheduleConfig implements ScheduleConfigAdapter {

    private ScheduleConfig config;

    public DefaultScheduleConfig(ScheduleConfig config){
        this.config = config;
    }

    /**
     * 配置定时任务
     */
    @Override
    public ScheduleConfig setConfig(){
        if(Objects.isNull(config)){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }

        ScheduleConfig config = getConfig(this.config.getPrefix());
        if(Objects.nonNull(config)){
            this.config = config;
        }

        String cron = this.config.getCron();
        if(Objects.isNull(cron) || cron.isEmpty()){
            throw new ExceptionWrapper(ExceptionEnum.ILLEGAL_ARGUMENT);
        }
        return this.config;
    }

    public static ScheduleConfig getConfig(String prefix){
        if(Objects.nonNull(prefix) && !prefix.isEmpty()){
            //优先查找配置
            Map<String, Object> map = ConfigurationProcessor.getInstance().getByPrefix(prefix);
            if(!map.isEmpty()){
                return JSONObject.parseObject(JSONObject.toJSONString(map),ScheduleConfig.class);
            }
        }
        return null;
    }

    @Override
    public JobDataMap setData() {
        return null;
    }

}

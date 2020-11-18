package com.boot.mini.core.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

/**
 * @author xhzy
 */
public class DateUtil {

    public static Date parse(String s, String...patterns){
        if(Objects.isNull(s) || s.isEmpty()){
            return null;
        }

        LocalDateTime dateTime = LocalDateTime.parse(s, DateTimeFormatter.ofPattern(getPattern(patterns)).withZone(ZoneId.systemDefault()));
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    public static String format(Date date,String...patterns){
        if(Objects.isNull(date)){
            return null;
        }
        return DateTimeFormatter.ofPattern(getPattern(patterns)).format(LocalDateTime.ofInstant(date.toInstant(),ZoneId.systemDefault()));
    }

    private static String getPattern(String...patterns){
        String pattern = "yyyy-MM-dd HH:mm:ss";
        if(Objects.nonNull(patterns) && patterns.length > 0){
            pattern = patterns[0];
        }
        return pattern;
    }
}

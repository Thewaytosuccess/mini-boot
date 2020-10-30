package com.mvc.util;

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

        LocalDateTime dateTime = LocalDateTime.parse(s, DateTimeFormatter.ofPattern(getPattern(patterns))
                .withZone(ZoneId.systemDefault()));
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    public static String format(Date date,String...patterns){
        if(Objects.isNull(date)){
            return null;
        }
        return DateTimeFormatter.ofPattern(getPattern(patterns)).format(date.toInstant());
    }

    private static String getPattern(String...patterns){
        String pattern = "yyyy-MM-dd HH:mm:ss";
        if(patterns.length > 0){
            pattern = patterns[0];
        }
        return pattern;
    }
}

package com.mvc.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author xhzy
 */
public class DateUtil {

    public static Date parse(String s, String...pattern){
        if(pattern.length > 0){

        }
        LocalDateTime dateTime = LocalDateTime.parse(s, DateTimeFormatter.ofPattern(s).withZone(
                ZoneId.systemDefault()));
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }
}

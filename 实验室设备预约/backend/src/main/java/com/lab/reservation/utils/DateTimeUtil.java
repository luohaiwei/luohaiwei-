package com.lab.reservation.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 时间格式化工具类
 * 统一处理系统中所有的时间格式
 */
@Component
public class DateTimeUtil {

    /**
     * 标准日期时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 日期格式：yyyy-MM-dd
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * 时间格式：HH:mm:ss
     */
    public static final String TIME_FORMAT = "HH:mm:ss";

    /**
     * 短日期时间格式：yyyy-MM-dd HH:mm
     */
    public static final String DATE_TIME_SHORT_FORMAT = "yyyy-MM-dd HH:mm";

    /**
     * 将LocalDateTime转换为指定格式的字符串
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DATE_TIME_FORMAT);
    }

    /**
     * 将LocalDateTime转换为指定格式的字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 将字符串解析为LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr) {
        return parse(dateTimeStr, DATE_TIME_FORMAT);
    }

    /**
     * 将字符串解析为LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeStr, String pattern) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取当前时间的字符串表示
     */
    public static String now() {
        return format(LocalDateTime.now());
    }

    /**
     * 获取当前日期的字符串表示
     */
    public static String today() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    /**
     * 判断两个时间区间是否有交集（用于预约冲突检测）
     * @param start1 区间1开始时间
     * @param end1 区间1结束时间
     * @param start2 区间2开始时间
     * @param end2 区间2结束时间
     * @return 有交集返回true，否则返回false
     */
    public static boolean hasOverlap(String start1, String end1, String start2, String end2) {
        int s1 = timeToInt(start1);
        int e1 = timeToInt(end1);
        int s2 = timeToInt(start2);
        int e2 = timeToInt(end2);
        return !(e1 <= s2 || e2 <= s1);
    }

    /**
     * 将时间字符串转换为整数（用于比较）
     */
    private static int timeToInt(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}

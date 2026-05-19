package com.lab.reservation.utils;

import cn.hutool.core.util.IdUtil;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 通用工具类
 * 提供各种通用工具方法
 */
public class CommonUtil {

    /**
     * 生成唯一订单号
     */
    public static String generateOrderNo() {
        return "ORD" + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * 生成唯一工单编号
     */
    public static String generateWorkOrderNo() {
        return "WO" + IdUtil.getSnowflakeNextIdStr();
    }

    /**
     * MD5加密
     */
    public static String md5(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(str.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5加密失败", e);
        }
    }
}

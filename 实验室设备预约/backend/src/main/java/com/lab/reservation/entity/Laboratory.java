package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 实验室信息实体类
 */
@Data
@TableName("laboratory")
public class Laboratory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 实验室编码 */
    private String labCode;

    /** 实验室名称 */
    private String labName;

    /** 实验室类型（物理/化学/生物/计算机/其他） */
    private String labType;

    /** 实验室位置 */
    private String location;

    /** 所在楼层 */
    private String floor;

    /** 所在建筑 */
    private String building;

    /** 面积（平方米） */
    private BigDecimal area;

    /** 容纳人数 */
    private Integer capacity;

    /** 负责人姓名 */
    private String responsibleName;

    /** 负责人电话 */
    private String responsiblePhone;

    /** 负责人邮箱 */
    private String responsibleEmail;

    /** 开放时间开始 */
    private String openTime;

    /** 开放时间结束 */
    private String closeTime;

    /** 实验室简介 */
    private String description;

    /** 设备数量 */
    private Integer equipmentCount;

    /** 状态：0-停用，1-正常 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}

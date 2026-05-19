package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 设备信息实体类
 * 对应数据库表：device_info
 */
@Data
@TableName("device_info")
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备编号
     */
    private String deviceNo;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型ID
     */
    private Long categoryId;

    /**
     * 设备类型名称（非数据库字段）
     */
    @TableField(exist = false)
    private String categoryName;

    /**
     * 设备型号
     */
    private String model;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 购买日期（仅日期；前端 value-format yyyy-MM-dd 可正确反序列化）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate purchaseDate;

    /**
     * 设备价格
     */
    private Double price;

    /**
     * 所在实验室
     */
    private String laboratory;

    /**
     * 详细位置
     */
    private String location;

    /**
     * 精度等级：1-低，2-中，3-高
     */
    private Integer precisionLevel;

    /**
     * 状态：0-空闲，1-使用中，2-维修中，3-校准中，4-报废
     */
    private Integer status;

    /**
     * 设备简介
     */
    private String description;

    /**
     * 设备图片路径
     */
    private String imagePath;

    /**
     * 操作手册路径
     */
    private String manualPath;

    /**
     * 适配实验项目
     */
    private String adaptProject;

    /**
     * 校准周期（天）
     */
    private Integer calibrationCycle;

    /**
     * 下次校准时间
     */
    private LocalDateTime nextCalibrationDate;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志
     */
    @TableLogic
    private Integer deleted;
}

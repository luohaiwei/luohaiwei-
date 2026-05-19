package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device_scrap_application")
public class DeviceScrapApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long deviceId;
    private String deviceName;
    private String deviceNo;
    private String scrapReason;
    /** 0申请中 1通过 2拒绝 3归档 */
    private Integer status;
    private String auditOpinion;
    private Long auditorId;
    private LocalDateTime auditTime;

    @TableField(exist = false)
    private String applicant;     // 申请人姓名（非DB字段）

    @TableField(exist = false)
    private String auditor;      // 审批人姓名（非DB字段）

    @TableField(exist = false)
    private String model;        // 设备型号（非DB字段）

    @TableField(exist = false)
    private LocalDateTime purchaseDate;  // 购买日期（非DB字段）

    @TableField(exist = false)
    private Double purchasePrice; // 购买价格（非DB字段）

    @TableField(exist = false)
    private Integer usageYears;  // 使用年限（非DB字段）

    private Long applicantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}

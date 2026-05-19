package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 校准记录实体类
 * 对应数据库表：calibration_record
 */
@Data
@TableName("calibration_record")
public class CalibrationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 设备名称（非数据库字段）
     */
    @TableField(exist = false)
    private String deviceName;

    /**
     * 设备编号（非数据库字段）
     */
    @TableField(exist = false)
    private String deviceNo;

    /**
     * 校准时间
     */
    private LocalDateTime calibrationDate;

    /**
     * 校准周期（天）
     */
    private Integer calibrationCycle;

    /**
     * 下次校准时间
     */
    private LocalDateTime nextCalibrationDate;

    /**
     * 校准结果：0-不合格，1-合格
     */
    private Integer result;

    /**
     * 校准报告路径
     */
    private String reportPath;

    /**
     * 校准人ID
     */
    private Long calibratorId;

    /**
     * 校准人姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String calibratorName;

    /**
     * 校准备注
     */
    private String remark;

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

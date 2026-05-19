package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 预约订单实体类
 * 对应数据库表：booking_order
 */
@Data
@TableName("booking_order")
public class BookingOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 预约单号
     */
    private String orderNo;

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
     * 申请人ID
     */
    private Long userId;

    /**
     * 申请人姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 预约日期
     */
    private LocalDateTime bookingDate;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 使用时长（小时）
     */
    private Double duration;

    /**
     * 参与人数
     */
    private Integer participantCount;

    /**
     * 实验项目名称
     */
    private String experimentProject;

    /**
     * 预约事由
     */
    private String reason;

    /**
     * 状态：0-待审核，1-已通过，2-已拒绝，3-已完成，4-已取消
     */
    private Integer status;

    /**
     * 审核人ID
     */
    private Long auditorId;

    /**
     * 审核人姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String auditorName;

    /**
     * 审核意见
     */
    private String auditOpinion;

    /**
     * 审核时间
     */
    private LocalDateTime auditTime;

    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;

    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;

    /**
     * 使用评价
     */
    private String evaluation;

    /**
     * 评分（1-5）
     */
    private Integer rating;

    /**
     * 替换设备ID（用于设备替换申请）
     */
    private Long replaceDeviceId;

    /**
     * 替换设备名称（非数据库字段）
     */
    @TableField(exist = false)
    private String replaceDeviceName;

    /**
     * 替换原因
     */
    private String replaceReason;

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

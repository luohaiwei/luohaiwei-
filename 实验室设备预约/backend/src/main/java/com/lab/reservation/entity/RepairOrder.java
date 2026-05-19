package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 维修工单实体类
 * 对应数据库表：repair_order
 */
@Data
@TableName("repair_order")
public class RepairOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 工单编号
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
     * 报修人ID
     */
    private Long reporterId;

    /**
     * 报修人姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String reporterName;

    /**
     * 故障描述
     */
    private String faultDescription;

    /**
     * 故障图片路径
     */
    private String imagePath;

    /**
     * 报修时间
     */
    private LocalDateTime reportTime;

    /**
     * 状态：0-待处理，1-处理中，2-已完成，3-已关闭
     */
    private Integer status;

    /**
     * 维修人员ID
     */
    private Long handlerId;

    /**
     * 维修人员姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String handlerName;

    /**
     * 监控用：预计处理截止时间（非数据库字段；待接单按创建时间+48h，处理中按开始维修时间+72h）
     */
    @TableField(exist = false)
    private LocalDateTime deadline;

    /**
     * 优先级：URGENT-紧急，HIGH-高，NORMAL-普通，LOW-低
     */
    private String priority;

    /**
     * 维修开始时间
     */
    private LocalDateTime handleStartTime;

    /**
     * 维修完成时间
     */
    private LocalDateTime handleEndTime;

    /**
     * 故障原因
     */
    private String faultCause;

    /**
     * 维修方案
     */
    private String repairSolution;

    /**
     * 更换配件
     */
    private String replaceParts;

    /**
     * 维修费用
     */
    private Double repairCost;

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

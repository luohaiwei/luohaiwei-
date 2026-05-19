package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备状态变更日志实体
 */
@Data
@TableName("device_status_log")
public class DeviceStatusLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long deviceId;
    private String deviceNo;
    private String deviceName;
    private Integer oldStatus;
    private Integer newStatus;
    private Long operatorId;
    private String operatorName;
    private String changeReason;

    /**
     * 操作端 IP。标记为 exist=false：旧库无 operator_ip 列时 SELECT/INSERT 均不生成该列，避免查询整表报错。
     * 新库可执行 database/alter_device_status_log_operator_ip.sql 后，再改回 {@code @TableField("operator_ip")} 以持久化 IP。
     */
    @TableField(exist = false)
    private String operatorIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

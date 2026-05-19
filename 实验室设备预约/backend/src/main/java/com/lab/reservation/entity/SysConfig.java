package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 对应数据库表：sys_config
 */
@Data
@TableName("sys_config")
public class SysConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值(JSON格式存储) */
    private String configValue;

    /** 值类型：STRING, INT, BOOLEAN, JSON */
    private String configType;

    /** 配置名称 */
    private String configName;

    /** 分组：SYSTEM, NOTIFICATION, BACKUP, BOOKING */
    private String configGroup;

    /** 描述 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

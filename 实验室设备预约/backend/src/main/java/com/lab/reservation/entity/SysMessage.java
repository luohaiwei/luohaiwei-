package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统消息实体类
 * 对应数据库表：sys_message
 */
@Data
@TableName("sys_message")
public class SysMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 用户名（非数据库字段）
     */
    @TableField(exist = false)
    private String username;

    /**
     * 真实姓名（非数据库字段）
     */
    @TableField(exist = false)
    private String realName;

    /**
     * 消息类型：BOOKING_AUDIT-预约审核，REPAIR_ASSIGN-工单分配，CALIBRATION_REMIND-校准提醒，SYSTEM-系统通知
     */
    private String type;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 是否已读：0-未读，1-已读
     */
    private Integer isRead;

    /**
     * 关联业务ID（如预约ID、工单ID）
     */
    private Long relatedId;

    /**
     * 关联业务类型：booking_order，repair_order
     */
    private String relatedType;

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

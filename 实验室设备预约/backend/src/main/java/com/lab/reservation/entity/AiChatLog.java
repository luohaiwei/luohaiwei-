package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天记录实体类
 * 对应数据库表：ai_chat_log
 */
@Data
@TableName("ai_chat_log")
public class AiChatLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI回答
     */
    private String answer;

    /**
     * 关联知识库ID
     */
    private Long knowledgeId;

    /**
     * 设备ID（如果有）
     */
    private Long deviceId;

    /**
     * 匹配置信度（0-1）
     */
    private Double confidence;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String username;

    @TableField(exist = false)
    private String userType;

    /** DEVICE_OPERATION / EXPERIMENT_PROCESS / SAFETY / TROUBLESHOOTING */
    @TableField(exist = false)
    private String sessionType;

    /** NORMAL / ABNORMAL / LOW_QUALITY */
    @TableField(exist = false)
    private String quality;

    @TableField(exist = false)
    private Boolean safetyAlert;

    @TableField(exist = false)
    private String safetyAlertText;
}

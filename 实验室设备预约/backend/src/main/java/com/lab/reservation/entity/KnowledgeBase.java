package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库实体类
 * 对应数据库表：knowledge_base
 */
@Data
@TableName("knowledge_base")
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 知识分类
     */
    private String category;

    /**
     * 关联设备ID
     */
    private Long deviceId;

    /**
     * 关联设备名称（非数据库字段）
     */
    @TableField(exist = false)
    private String deviceName;

    /**
     * 问题标题
     */
    private String question;

    /**
     * 答案内容
     */
    private String answer;

    /**
     * 关键词（用于检索匹配）
     */
    private String keywords;

    /**
     * 是否为危险操作提醒：0-否，1-是
     */
    private Integer isDanger;

    /**
     * 引导类型：TEXT-纯文本, IMAGE-图文引导
     */
    private String guideType;

    /**
     * 引导图片URL列表（JSON数组）
     */
    private String guideImages;

    /**
     * 分步骤引导内容（JSON格式）
     * 结构：[{"step":1,"title":"步骤标题","content":"步骤内容","image":"图片URL","warning":"安全警示"}]
     */
    private String guideSteps;

    /**
     * 预计完成时长（分钟）
     */
    private Integer guideDuration;

    /**
     * 排序号
     */
    private Integer sort;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

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

package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识图谱节点实体
 * 节点类型：DEVICE-设备，KNOWLEDGE-知识，CATEGORY-分类
 */
@Data
@TableName("knowledge_graph_node")
public class KnowledgeGraphNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 节点类型：DEVICE/KNOWLEDGE/CATEGORY */
    private String nodeType;

    /** 关联实体ID（device_info.id 或 knowledge_base.id） */
    private Long nodeId;

    /** 节点名称 */
    private String nodeName;

    /** 节点标签 */
    private String nodeLabel;

    /** 描述 */
    private String description;

    /** 扩展数据（JSON格式） */
    private String extraData;

    /** 布局X坐标 */
    private BigDecimal xPosition;

    /** 布局Y坐标 */
    private BigDecimal yPosition;

    /** 节点大小 */
    private Integer size;

    /** 节点颜色 */
    private String color;

    /** 排序号 */
    private Integer sort;

    /** 状态：0-隐藏，1-显示 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;
}

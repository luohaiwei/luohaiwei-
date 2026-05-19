package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识图谱边实体
 * 边类型：BELONGS_TO-属于，RELATED_TO-关联，CONTAINS-包含，DEPENDS_ON-依赖
 */
@Data
@TableName("knowledge_graph_edge")
public class KnowledgeGraphEdge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 源节点ID */
    private Long sourceNodeId;

    /** 目标节点ID */
    private Long targetNodeId;

    /** 边类型 */
    private String edgeType;

    /** 边标签 */
    private String edgeLabel;

    /** 权重 */
    private BigDecimal weight;

    /** 关系描述 */
    private String description;

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

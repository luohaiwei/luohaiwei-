package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.KnowledgeGraphNode;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱Service接口
 */
public interface KnowledgeGraphService extends IService<KnowledgeGraphNode> {

    /**
     * 获取图谱全量数据（节点+边）
     * @return Map 包含 nodes 和 edges
     */
    Map<String, Object> getGraphData();

    /**
     * 根据节点ID获取关联节点
     */
    Map<String, Object> getNodeNeighbors(Long nodeId);

    /**
     * 获取图谱统计信息
     */
    Map<String, Object> getGraphStats();
}

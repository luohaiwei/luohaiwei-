package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.KnowledgeGraphEdge;
import com.lab.reservation.entity.KnowledgeGraphNode;
import com.lab.reservation.mapper.KnowledgeGraphEdgeMapper;
import com.lab.reservation.mapper.KnowledgeGraphNodeMapper;
import com.lab.reservation.service.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱Service实现
 */
@Service
public class KnowledgeGraphServiceImpl extends ServiceImpl<KnowledgeGraphNodeMapper, KnowledgeGraphNode> implements KnowledgeGraphService {

    @Autowired
    private KnowledgeGraphNodeMapper nodeMapper;

    @Autowired
    private KnowledgeGraphEdgeMapper edgeMapper;

    @Override
    public Map<String, Object> getGraphData() {
        List<KnowledgeGraphNode> nodes = nodeMapper.selectAllVisible();
        List<KnowledgeGraphEdge> edges = edgeMapper.selectAllVisible();

        // 构建节点Map方便查找
        Map<Long, KnowledgeGraphNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(KnowledgeGraphNode::getId, n -> n));

        // 转换为前端所需格式
        List<Map<String, Object>> graphNodes = nodes.stream().map(node -> {
            Map<String, Object> n = new LinkedHashMap<>();
            n.put("id", node.getId());
            n.put("nodeType", node.getNodeType());
            n.put("nodeId", node.getNodeId());
            n.put("name", node.getNodeName());
            n.put("label", node.getNodeLabel());
            n.put("description", node.getDescription());
            n.put("extraData", node.getExtraData());

            // 位置信息
            Map<String, Object> position = new LinkedHashMap<>();
            position.put("x", node.getXPosition() != null ? node.getXPosition().doubleValue() : 0);
            position.put("y", node.getYPosition() != null ? node.getYPosition().doubleValue() : 0);
            n.put("position", position);

            n.put("size", node.getSize() != null ? node.getSize() : 40);
            n.put("color", getNodeColor(node.getNodeType(), node.getColor()));

            // 节点样式
            Map<String, Object> style = new LinkedHashMap<>();
            style.put("borderWidth", 2);
            style.put("borderColor", getNodeBorderColor(node.getNodeType()));
            style.put("backgroundColor", getNodeBackgroundColor(node.getNodeType()));
            n.put("style", style);

            return n;
        }).collect(Collectors.toList());

        // 边：过滤掉指向已删除节点的边
        Set<Long> validNodeIds = nodes.stream().map(KnowledgeGraphNode::getId).collect(Collectors.toSet());
        List<Map<String, Object>> graphEdges = edges.stream()
                .filter(e -> validNodeIds.contains(e.getSourceNodeId()) && validNodeIds.contains(e.getTargetNodeId()))
                .map(edge -> {
                    Map<String, Object> e = new LinkedHashMap<>();
                    e.put("id", edge.getId());
                    e.put("source", edge.getSourceNodeId());
                    e.put("target", edge.getTargetNodeId());
                    e.put("edgeType", edge.getEdgeType());
                    e.put("label", edge.getEdgeLabel());
                    e.put("weight", edge.getWeight() != null ? edge.getWeight().doubleValue() : 1.0);
                    e.put("description", edge.getDescription());

                    // 边的样式
                    Map<String, Object> style = new LinkedHashMap<>();
                    style.put("strokeColor", getEdgeColor(edge.getEdgeType()));
                    style.put("strokeWidth", getEdgeStrokeWidth(edge.getEdgeType()));
                    e.put("style", style);

                    return e;
                }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nodes", graphNodes);
        result.put("edges", graphEdges);
        return result;
    }

    @Override
    public Map<String, Object> getNodeNeighbors(Long nodeId) {
        KnowledgeGraphNode centerNode = nodeMapper.selectById(nodeId);
        if (centerNode == null) {
            return Collections.emptyMap();
        }

        // 找所有相邻边
        QueryWrapper<KnowledgeGraphEdge> edgeWrapper = new QueryWrapper<>();
        edgeWrapper.and(w -> w.eq("source_node_id", nodeId).or().eq("target_node_id", nodeId));
        edgeWrapper.eq("status", 1).eq("deleted", 0);
        List<KnowledgeGraphEdge> edges = edgeMapper.selectList(edgeWrapper);

        // 收集相邻节点ID
        Set<Long> neighborIds = new HashSet<>();
        for (KnowledgeGraphEdge e : edges) {
            if (e.getSourceNodeId().equals(nodeId)) {
                neighborIds.add(e.getTargetNodeId());
            } else {
                neighborIds.add(e.getSourceNodeId());
            }
        }

        // 查询相邻节点
        List<KnowledgeGraphNode> neighbors = new ArrayList<>();
        if (!neighborIds.isEmpty()) {
            neighbors = nodeMapper.selectList(
                    new QueryWrapper<KnowledgeGraphNode>()
                            .in("id", neighborIds)
                            .eq("status", 1)
                            .eq("deleted", 0)
            );
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("center", centerNode);
        result.put("neighbors", neighbors);
        result.put("edges", edges);
        return result;
    }

    @Override
    public Map<String, Object> getGraphStats() {
        List<KnowledgeGraphNode> nodes = nodeMapper.selectAllVisible();
        List<KnowledgeGraphEdge> edges = edgeMapper.selectAllVisible();

        long deviceCount = nodes.stream().filter(n -> "DEVICE".equals(n.getNodeType())).count();
        long knowledgeCount = nodes.stream().filter(n -> "KNOWLEDGE".equals(n.getNodeType())).count();
        long categoryCount = nodes.stream().filter(n -> "CATEGORY".equals(n.getNodeType())).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalNodes", nodes.size());
        stats.put("totalEdges", edges.size());
        stats.put("deviceCount", deviceCount);
        stats.put("knowledgeCount", knowledgeCount);
        stats.put("categoryCount", categoryCount);
        return stats;
    }

    private String getNodeColor(String nodeType, String customColor) {
        if (customColor != null && !customColor.isEmpty()) return customColor;
        switch (nodeType) {
            case "DEVICE": return "#409EFF";
            case "KNOWLEDGE": return "#67C23A";
            case "CATEGORY": return "#7B61FF";
            default: return "#909399";
        }
    }

    private String getNodeBorderColor(String nodeType) {
        switch (nodeType) {
            case "DEVICE": return "#66B1FF";
            case "KNOWLEDGE": return "#85CE61";
            case "CATEGORY": return "#A28BFF";
            default: return "#B1B3B8";
        }
    }

    private String getNodeBackgroundColor(String nodeType) {
        switch (nodeType) {
            case "DEVICE": return "rgba(64, 158, 255, 0.15)";
            case "KNOWLEDGE": return "rgba(103, 194, 58, 0.15)";
            case "CATEGORY": return "rgba(123, 97, 255, 0.15)";
            default: return "rgba(144, 147, 153, 0.15)";
        }
    }

    private String getEdgeColor(String edgeType) {
        switch (edgeType) {
            case "BELONGS_TO": return "#7B61FF";
            case "RELATED_TO": return "#E6A23C";
            case "CONTAINS": return "#409EFF";
            case "DEPENDS_ON": return "#F56C6C";
            default: return "#909399";
        }
    }

    private int getEdgeStrokeWidth(String edgeType) {
        switch (edgeType) {
            case "BELONGS_TO": return 2;
            case "RELATED_TO": return 2;
            case "CONTAINS": return 1;
            case "DEPENDS_ON": return 2;
            default: return 1;
        }
    }
}

package com.lab.reservation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DeviceInfo;
import com.lab.reservation.entity.KnowledgeBase;
import com.lab.reservation.entity.KnowledgeGraphEdge;
import com.lab.reservation.entity.KnowledgeGraphNode;
import com.lab.reservation.mapper.KnowledgeGraphEdgeMapper;
import com.lab.reservation.mapper.KnowledgeGraphNodeMapper;
import com.lab.reservation.service.DeviceInfoService;
import com.lab.reservation.service.KnowledgeBaseService;
import com.lab.reservation.service.KnowledgeGraphService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱Controller
 */
@RestController
@RequestMapping("/knowledge-graph")
public class KnowledgeGraphController {

    @Autowired
    private KnowledgeGraphService knowledgeGraphService;

    @Autowired
    private KnowledgeGraphNodeMapper nodeMapper;

    @Autowired
    private KnowledgeGraphEdgeMapper edgeMapper;

    @Autowired
    private DeviceInfoService deviceInfoService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 获取图谱全量数据（节点+边）
     */
    @GetMapping("/data")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getGraphData() {
        return knowledgeGraphService.getGraphData();
    }

    /**
     * 根据节点ID获取关联节点详情
     */
    @GetMapping("/node/{nodeId}/neighbors")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getNodeNeighbors(@PathVariable Long nodeId) {
        return knowledgeGraphService.getNodeNeighbors(nodeId);
    }

    /**
     * 获取图谱统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getGraphStats() {
        return knowledgeGraphService.getGraphStats();
    }

    /**
     * 图谱管理下拉选项（设备/知识/分类/现有节点）
     */
    @GetMapping("/manage/options")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> manageOptions() {
        Map<String, Object> result = new HashMap<>();

        List<DeviceInfo> devices = deviceInfoService.list(
                new QueryWrapper<DeviceInfo>().select("id", "device_name", "device_no").orderByAsc("id")
        );
        List<Map<String, Object>> deviceOptions = devices.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("name", d.getDeviceName());
            m.put("code", d.getDeviceNo());
            return m;
        }).collect(Collectors.toList());

        List<KnowledgeBase> knowledgeList = knowledgeBaseService.list(
                new QueryWrapper<KnowledgeBase>().select("id", "question", "category", "device_id").orderByAsc("id")
        );
        List<Map<String, Object>> knowledgeOptions = knowledgeList.stream().map(k -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", k.getId());
            m.put("name", k.getQuestion());
            m.put("category", k.getCategory());
            m.put("deviceId", k.getDeviceId());
            return m;
        }).collect(Collectors.toList());

        // 分类来源：知识库 category + 图谱已存在 CATEGORY 节点名
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (KnowledgeBase kb : knowledgeList) {
            if (kb.getCategory() != null && !kb.getCategory().trim().isEmpty()) {
                categories.add(kb.getCategory().trim());
            }
        }
        List<KnowledgeGraphNode> categoryNodes = nodeMapper.selectList(
                new QueryWrapper<KnowledgeGraphNode>().eq("node_type", "CATEGORY").orderByAsc("id")
        );
        for (KnowledgeGraphNode n : categoryNodes) {
            if (n.getNodeName() != null && !n.getNodeName().trim().isEmpty()) {
                categories.add(n.getNodeName().trim());
            }
        }

        List<KnowledgeGraphNode> allNodes = nodeMapper.selectList(
                new QueryWrapper<KnowledgeGraphNode>().select("id", "node_type", "node_name", "node_label", "node_id").orderByAsc("id")
        );
        List<Map<String, Object>> nodeOptions = allNodes.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("nodeType", n.getNodeType());
            m.put("nodeName", n.getNodeName());
            m.put("nodeLabel", n.getNodeLabel());
            m.put("nodeId", n.getNodeId());
            return m;
        }).collect(Collectors.toList());

        result.put("devices", deviceOptions);
        result.put("knowledge", knowledgeOptions);
        result.put("categories", new ArrayList<>(categories));
        result.put("nodes", nodeOptions);
        return result;
    }

    // ==================== 手动管理：节点 ====================

    @GetMapping("/manage/nodes")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> listNodes(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String nodeType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        QueryWrapper<KnowledgeGraphNode> qw = new QueryWrapper<>();
        if (nodeType != null && !nodeType.trim().isEmpty()) {
            qw.eq("node_type", nodeType.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            qw.and(w -> w.like("node_name", k)
                    .or().like("node_label", k)
                    .or().like("description", k));
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByAsc("id");

        Page<KnowledgeGraphNode> page = nodeMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    @PostMapping("/manage/nodes")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> addNode(@RequestBody KnowledgeGraphNode node) {
        Map<String, Object> result = new HashMap<>();
        String validMsg = validateNode(node, null);
        if (validMsg != null) {
            result.put("message", validMsg);
            return result;
        }
        if (node.getStatus() == null) {
            node.setStatus(1);
        }
        if (node.getSort() == null) {
            node.setSort(0);
        }
        nodeMapper.insert(node);
        result.put("message", "添加成功");
        return result;
    }

    @PutMapping("/manage/nodes/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> updateNode(@PathVariable Long id, @RequestBody KnowledgeGraphNode node) {
        Map<String, Object> result = new HashMap<>();
        KnowledgeGraphNode db = nodeMapper.selectById(id);
        if (db == null) {
            result.put("message", "节点不存在");
            return result;
        }

        String validMsg = validateNode(node, id);
        if (validMsg != null) {
            result.put("message", validMsg);
            return result;
        }

        node.setId(id);
        nodeMapper.updateById(node);
        result.put("message", "更新成功");
        return result;
    }

    @PutMapping("/manage/nodes/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> updateNodeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Map<String, Object> result = new HashMap<>();
        KnowledgeGraphNode db = nodeMapper.selectById(id);
        if (db == null) {
            result.put("message", "节点不存在");
            return result;
        }
        Integer status = body == null ? null : body.get("status");
        db.setStatus(status != null ? status : 1);
        nodeMapper.updateById(db);
        result.put("message", "状态更新成功");
        return result;
    }

    @DeleteMapping("/manage/nodes/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> deleteNode(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        KnowledgeGraphNode db = nodeMapper.selectById(id);
        if (db == null) {
            result.put("message", "节点不存在");
            return result;
        }

        QueryWrapper<KnowledgeGraphEdge> ew = new QueryWrapper<>();
        ew.and(w -> w.eq("source_node_id", id).or().eq("target_node_id", id));
        edgeMapper.delete(ew);
        nodeMapper.deleteById(id);

        result.put("message", "删除成功");
        return result;
    }

    // ==================== 手动管理：关系 ====================

    @GetMapping("/manage/edges")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> listEdges(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String edgeType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {

        QueryWrapper<KnowledgeGraphEdge> qw = new QueryWrapper<>();
        if (edgeType != null && !edgeType.trim().isEmpty()) {
            qw.eq("edge_type", edgeType.trim());
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            qw.and(w -> w.like("edge_label", k)
                    .or().like("description", k));
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByAsc("id");

        Page<KnowledgeGraphEdge> page = edgeMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        return result;
    }

    @PostMapping("/manage/edges")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> addEdge(@RequestBody KnowledgeGraphEdge edge) {
        Map<String, Object> result = new HashMap<>();
        String validMsg = validateEdge(edge, null);
        if (validMsg != null) {
            result.put("message", validMsg);
            return result;
        }
        if (edge.getStatus() == null) {
            edge.setStatus(1);
        }
        if (edge.getSort() == null) {
            edge.setSort(0);
        }
        edgeMapper.insert(edge);
        result.put("message", "添加成功");
        return result;
    }

    @PutMapping("/manage/edges/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> updateEdge(@PathVariable Long id, @RequestBody KnowledgeGraphEdge edge) {
        Map<String, Object> result = new HashMap<>();
        KnowledgeGraphEdge db = edgeMapper.selectById(id);
        if (db == null) {
            result.put("message", "关系不存在");
            return result;
        }

        String validMsg = validateEdge(edge, id);
        if (validMsg != null) {
            result.put("message", validMsg);
            return result;
        }

        edge.setId(id);
        edgeMapper.updateById(edge);
        result.put("message", "更新成功");
        return result;
    }

    @PutMapping("/manage/edges/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> updateEdgeStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Map<String, Object> result = new HashMap<>();
        KnowledgeGraphEdge db = edgeMapper.selectById(id);
        if (db == null) {
            result.put("message", "关系不存在");
            return result;
        }
        Integer status = body == null ? null : body.get("status");
        db.setStatus(status != null ? status : 1);
        edgeMapper.updateById(db);
        result.put("message", "状态更新成功");
        return result;
    }

    @DeleteMapping("/manage/edges/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','LAB_ADMIN')")
    public Map<String, Object> deleteEdge(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        KnowledgeGraphEdge db = edgeMapper.selectById(id);
        if (db == null) {
            result.put("message", "关系不存在");
            return result;
        }
        edgeMapper.deleteById(id);
        result.put("message", "删除成功");
        return result;
    }

    private String validateNode(KnowledgeGraphNode node, Long selfId) {
        if (node == null) {
            return "请求参数不能为空";
        }
        String nodeType = node.getNodeType() == null ? "" : node.getNodeType().trim().toUpperCase();
        if (!("DEVICE".equals(nodeType) || "KNOWLEDGE".equals(nodeType) || "CATEGORY".equals(nodeType))) {
            return "节点类型不合法";
        }
        if (node.getNodeName() == null || node.getNodeName().trim().isEmpty()) {
            return "节点名称不能为空";
        }

        Long nodeId = node.getNodeId();
        if ("DEVICE".equals(nodeType)) {
            if (nodeId == null) return "设备节点必须选择关联设备";
            if (deviceInfoService.getById(nodeId) == null) return "关联设备不存在";
        } else if ("KNOWLEDGE".equals(nodeType)) {
            if (nodeId == null) return "知识节点必须选择关联知识";
            if (knowledgeBaseService.getById(nodeId) == null) return "关联知识不存在";
        } else {
            // CATEGORY 建议不关联 nodeId
            if (nodeId != null) {
                return "分类节点无需填写关联ID";
            }
        }

        // 唯一约束：同类型 + 同关联ID 不可重复（CATEGORY 不校验 nodeId）
        if (nodeId != null) {
            QueryWrapper<KnowledgeGraphNode> uq = new QueryWrapper<KnowledgeGraphNode>()
                    .eq("node_type", nodeType)
                    .eq("node_id", nodeId);
            if (selfId != null) {
                uq.ne("id", selfId);
            }
            if (nodeMapper.selectCount(uq) > 0) {
                return "该实体已存在对应图谱节点，请勿重复添加";
            }
        }

        node.setNodeType(nodeType);
        node.setNodeName(node.getNodeName().trim());
        if (node.getNodeLabel() != null) node.setNodeLabel(node.getNodeLabel().trim());
        if (node.getDescription() != null) node.setDescription(node.getDescription().trim());
        return null;
    }

    private String validateEdge(KnowledgeGraphEdge edge, Long selfId) {
        if (edge == null) {
            return "请求参数不能为空";
        }
        if (edge.getSourceNodeId() == null || edge.getTargetNodeId() == null) {
            return "源节点和目标节点不能为空";
        }
        if (edge.getSourceNodeId().equals(edge.getTargetNodeId())) {
            return "源节点和目标节点不能相同";
        }
        String edgeType = edge.getEdgeType() == null ? "" : edge.getEdgeType().trim().toUpperCase();
        if (!("BELONGS_TO".equals(edgeType) || "RELATED_TO".equals(edgeType)
                || "CONTAINS".equals(edgeType) || "DEPENDS_ON".equals(edgeType))) {
            return "关系类型不合法";
        }

        KnowledgeGraphNode src = nodeMapper.selectById(edge.getSourceNodeId());
        KnowledgeGraphNode tgt = nodeMapper.selectById(edge.getTargetNodeId());
        if (src == null || tgt == null) {
            return "源节点或目标节点不存在";
        }

        QueryWrapper<KnowledgeGraphEdge> uq = new QueryWrapper<KnowledgeGraphEdge>()
                .eq("source_node_id", edge.getSourceNodeId())
                .eq("target_node_id", edge.getTargetNodeId())
                .eq("edge_type", edgeType);
        if (selfId != null) {
            uq.ne("id", selfId);
        }
        if (edgeMapper.selectCount(uq) > 0) {
            return "相同关系已存在，请勿重复添加";
        }

        edge.setEdgeType(edgeType);
        if (edge.getEdgeLabel() != null) edge.setEdgeLabel(edge.getEdgeLabel().trim());
        if (edge.getDescription() != null) edge.setDescription(edge.getDescription().trim());
        return null;
    }
}

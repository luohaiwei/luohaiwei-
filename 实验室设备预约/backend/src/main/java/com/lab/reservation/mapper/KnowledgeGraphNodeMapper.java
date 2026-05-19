package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.KnowledgeGraphNode;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 知识图谱节点Mapper
 */
@Mapper
public interface KnowledgeGraphNodeMapper extends BaseMapper<KnowledgeGraphNode> {

    /**
     * 查询所有显示的节点
     */
    List<KnowledgeGraphNode> selectAllVisible();
}

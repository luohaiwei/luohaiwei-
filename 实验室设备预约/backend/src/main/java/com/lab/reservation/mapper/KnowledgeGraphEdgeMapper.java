package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.KnowledgeGraphEdge;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 知识图谱边Mapper
 */
@Mapper
public interface KnowledgeGraphEdgeMapper extends BaseMapper<KnowledgeGraphEdge> {

    /**
     * 查询所有显示的边
     */
    List<KnowledgeGraphEdge> selectAllVisible();
}

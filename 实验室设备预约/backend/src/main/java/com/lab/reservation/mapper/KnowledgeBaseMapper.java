package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 知识库Mapper接口
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 根据关键词搜索知识
     */
    List<KnowledgeBase> searchByKeywords(@Param("keyword") String keyword);

    /**
     * 根据分类查询知识列表
     */
    List<KnowledgeBase> selectByCategory(@Param("category") String category);

    /**
     * 查询危险操作提醒列表
     */
    List<KnowledgeBase> selectDangerAlerts();

    /**
     * 根据设备ID查询关联知识（该设备操作手册、注意事项等）
     */
    List<KnowledgeBase> selectByDeviceId(@Param("deviceId") Long deviceId);
}

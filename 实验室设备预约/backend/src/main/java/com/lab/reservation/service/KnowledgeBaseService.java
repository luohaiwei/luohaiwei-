package com.lab.reservation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.KnowledgeBase;
import java.util.List;
import java.util.Map;

/**
 * 知识库Service接口
 */
public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 智能问答
     */
    String smartQuery(String question, Long userId);

    /**
     * 智能问答（带危险等级，可按分类优先匹配）
     * @return Map 包含 answer 和 dangerLevel (danger/warning/info)
     */
    Map<String, Object> smartQueryWithDangerLevel(String question, Long userId, String category);

    /**
     * 根据关键词搜索
     */
    List<KnowledgeBase> searchByKeyword(String keyword);

    /**
     * 根据分类获取知识列表
     */
    List<KnowledgeBase> listByCategory(String category);

    /**
     * 获取危险操作提醒
     */
    List<KnowledgeBase> getDangerAlerts();

    /**
     * 根据设备ID查询关联知识列表
     */
    List<KnowledgeBase> listByDeviceId(Long deviceId);

    /**
     * 分页查询（管理员知识库管理）
     */
    IPage<KnowledgeBase> pageManage(int pageNum, int pageSize, String category, String keyword, Integer status);
}

package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.AiChatLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI聊天记录Mapper接口
 */
@Mapper
public interface AiChatLogMapper extends BaseMapper<AiChatLog> {

    IPage<AiChatLog> selectAuditPage(Page<AiChatLog> page,
            @Param("username") String username,
            @Param("userType") String userType,
            @Param("kbCategory") String kbCategory,
            @Param("quality") String quality,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    Map<String, Object> selectAuditStats(
            @Param("username") String username,
            @Param("userType") String userType,
            @Param("kbCategory") String kbCategory,
            @Param("quality") String quality,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Select("SELECT * FROM ai_chat_log WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT #{limit}")
    List<AiChatLog> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);
}

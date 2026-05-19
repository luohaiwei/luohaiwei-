package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统消息Mapper接口
 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {

    /**
     * 统计用户未读消息数量
     */
    int countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 将用户所有消息标记为已读
     */
    int markAllAsReadByUserId(@Param("userId") Long userId);
}

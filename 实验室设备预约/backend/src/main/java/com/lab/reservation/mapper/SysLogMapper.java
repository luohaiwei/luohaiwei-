package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 系统日志Mapper接口
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {

    /**
     * 获取用户登录次数
     * @param userId 用户ID
     * @param period 统计周期：day/week/month
     * @return 登录次数
     */
    Integer countUserLogins(@Param("userId") Long userId, @Param("period") String period);

    /**
     * 获取用户活跃度统计列表
     * @param days 统计天数
     * @param limit 返回数量限制
     * @return 用户活跃度统计列表
     */
    List<Map<String, Object>> selectUserActivityStats(@Param("days") Integer days, @Param("limit") Integer limit);

    Integer countDistinctLoginUsers(@Param("days") int days);

    List<Map<String, Object>> selectLoginDistinctTrend(@Param("days") int days);
}

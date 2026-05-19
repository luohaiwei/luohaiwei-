package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.BookingOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预约订单Mapper接口
 */
@Mapper
public interface BookingOrderMapper extends BaseMapper<BookingOrder> {

    /**
     * 根据设备ID和日期查询预约列表
     */
    List<BookingOrder> selectByDeviceAndDate(@Param("deviceId") Long deviceId,
                                            @Param("bookingDate") LocalDateTime bookingDate);

    /**
     * 根据用户ID查询预约列表
     */
    List<BookingOrder> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询待审核的预约列表
     */
    List<BookingOrder> selectPendingAudit();

    /**
     * 分页查询待审核预约（关联设备、申请人、拟替换设备，供审核列表展示）
     */
    IPage<BookingOrder> selectPendingAuditPage(Page<BookingOrder> page);

    /**
     * 查询设备在某时间段的预约（用于冲突检测）
     */
    List<BookingOrder> selectByDeviceAndTimeRange(@Param("deviceId") Long deviceId,
                                                   @Param("bookingDate") LocalDateTime bookingDate,
                                                   @Param("startTime") String startTime,
                                                   @Param("endTime") String endTime);

    /**
     * 查询预约统计
     */
    Map<String, Object> selectBookingStatistics();

    /**
     * 近7天预约趋势
     */
    List<Map<String, Object>> selectBookingTrend();

    /**
     * 预约高峰时段
     */
    List<Map<String, Object>> selectPeakHours();

    /**
     * 计算平均预约等待时长（从提交到审核完成的平均时长）
     * @param period 统计周期：day-今日，week-本周，month-本月
     * @return 平均等待时长（小时）
     */
    Double selectAvgWaitTime(@Param("period") String period);

    /**
     * 获取等待时长趋势数据（近7天/30天）
     * @param days 天数
     * @return 每日平均等待时长列表
     */
    List<Map<String, Object>> selectWaitTimeTrend(@Param("days") Integer days);

    /**
     * 获取用户预约统计
     * @param userId 用户ID
     * @param period 统计周期：day/week/month
     * @return 预约数量
     */
    Integer countUserBookings(@Param("userId") Long userId, @Param("period") String period);

    /**
     * 获取用户预约总时长（小时）
     * @param userId 用户ID
     * @param period 统计周期
     * @return 总时长
     */
    Double sumUserBookingDuration(@Param("userId") Long userId, @Param("period") String period);

    /**
     * 系统管理员：预约全局列表（分页，多条件）
     */
    IPage<BookingOrder> selectGlobalListPage(Page<BookingOrder> page,
            @Param("orderNo") String orderNo,
            @Param("deviceName") String deviceName,
            @Param("userName") String userName,
            @Param("status") Integer status,
            @Param("auditStatus") String auditStatus);

    /**
     * 近 N 天设备预约次数 TOP
     */
    List<Map<String, Object>> selectHotDevicesByBookings(@Param("days") int days, @Param("limit") int limit);

    /**
     * 近 N 天无预约使用的设备（闲置）
     */
    List<Map<String, Object>> selectIdleDevicesNotBooked(@Param("days") int days, @Param("limit") int limit);

    /**
     * 设备使用记录查询（已完成预约，关联设备名称/编号、使用人姓名）
     */
    java.util.List<java.util.Map<java.lang.String, java.lang.Object>> selectUsageRecords(
            @Param("deviceName") java.lang.String deviceName,
            @Param("userName") java.lang.String userName,
            @Param("startDate") java.lang.String startDate,
            @Param("endDate") java.lang.String endDate,
            @Param("laboratoryFilter") java.lang.String laboratoryFilter);
}

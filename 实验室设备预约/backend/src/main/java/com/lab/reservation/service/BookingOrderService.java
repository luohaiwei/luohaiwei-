package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.BookingOrder;
import com.lab.reservation.entity.DeviceInfo;

/**
 * 预约Service接口
 */
public interface BookingOrderService extends IService<BookingOrder> {

    /**
     * 创建预约
     */
    boolean createBooking(BookingOrder bookingOrder);

    /**
     * 校验预约是否符合全局规则（提前量、工作日段、次数/时长限额等）。
     * 改期时传入 excludeBookingId 以排除当前单，避免把自身计入当日次数。
     */
    void validateGlobalBookingConstraints(BookingOrder order, Long excludeBookingId);

    /**
     * 审核预约
     */
    boolean auditBooking(Long id, Long auditorId, Integer status, String opinion);

    /**
     * 完成预约（仅预约人本人，且状态为已通过）
     */
    boolean completeBooking(Long id, Long userId);

    /**
     * 根据是否存在「已通过」预约，将设备同步为使用中(1)或空闲(0)（维修结束等场景调用）
     */
    void syncDeviceStatusWithBookings(Long deviceId);

    /**
     * 取消预约
     */
    boolean cancelBooking(Long id, Long userId);

    /**
     * 管理员取消他人预约（调度/审核场景，需数据权限 CANCEL）
     */
    boolean adminCancelBooking(Long id, Long operatorId, String reason);

    /**
     * 检测预约冲突
     */
    boolean checkConflict(Long deviceId, java.time.LocalDateTime bookingDate, String startTime, String endTime);

    /**
     * 检测预约冲突（增强版）
     * @return 冲突信息，包含冲突类型和替代设备建议
     */
    java.util.Map<String, Object> checkConflictWithDetails(Long deviceId, java.time.LocalDateTime bookingDate, String startTime, String endTime);

    /**
     * 获取用户预约列表
     */
    Page<BookingOrder> pageUserBookings(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 获取待审核列表
     */
    Page<BookingOrder> pagePendingAudit(Integer pageNum, Integer pageSize);

    /**
     * 获取今日预约数
     */
    long getTodayBookingCount();

    /**
     * 获取本周预约数
     */
    long getWeekBookingCount();

    /**
     * 统计未删除预约总数（显式SQL，确保准确）
     */
    long countAllBookings();

    /**
     * 获取待审核数量
     */
    long countPendingAudit();

    /**
     * 近7天预约趋势
     */
    java.util.List<java.util.Map<String, Object>> getBookingTrend();

    /**
     * 预约高峰时段
     */
    java.util.List<java.util.Map<String, Object>> getPeakHours();

    /**
     * 获取平均预约等待时长
     * @param period 统计周期：day-今日，week-本周，month-本月
     * @return 平均等待时长（小时）
     */
    Double getAvgWaitTime(String period);

    /**
     * 获取等待时长趋势数据
     * @param days 天数
     * @return 每日平均等待时长列表
     */
    java.util.List<java.util.Map<String, Object>> getWaitTimeTrend(Integer days);

    /**
     * 获取用户预约数量
     * @param userId 用户ID
     * @param period 统计周期
     * @return 预约数量
     */
    Integer getUserBookingCount(Long userId, String period);

    /**
     * 获取用户预约总时长
     * @param userId 用户ID
     * @param period 统计周期
     * @return 总时长（小时）
     */
    Double getUserBookingDuration(Long userId, String period);

    /**
     * 申请设备替换
     * @param bookingId 预约ID
     * @param newDeviceId 新设备ID
     * @param reason 替换原因
     * @param userId 申请用户ID
     * @return 是否成功
     */
    boolean applyDeviceReplace(Long bookingId, Long newDeviceId, String reason, Long userId);

    /**
     * 获取可替换的设备列表
     * @param originalDeviceId 原设备ID
     * @param bookingDate 预约日期
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 可用设备列表
     */
    java.util.List<DeviceInfo> getReplaceableDevices(Long originalDeviceId, java.time.LocalDateTime bookingDate, String startTime, String endTime);

    /**
     * 系统管理员：预约全局列表分页
     */
    Page<BookingOrder> pageGlobalBookings(Integer pageNum, Integer pageSize,
            String orderNo, String deviceName, String userName, Integer status, String auditStatus);

    /**
     * 系统管理员：强制关闭预约（置为已取消并释放设备占用）
     */
    boolean adminForceCloseBooking(Long id);

    /**
     * 管理员：标记预约已完成（置为已使用，正常结束）
     * @param id 预约ID
     * @param operatorId 操作用户ID
     * @return 是否成功
     */
    boolean adminMarkComplete(Long id, Long operatorId);

    /**
     * 管理员：标记预约爽约（置为已完成并累加用户爽约计数，超阈值时禁止其再预约）
     * @param id 预约ID
     * @param operatorId 操作用户ID
     * @return 是否成功
     */
    boolean adminMarkNoShow(Long id, Long operatorId);

    /**
     * 数据分析：热门设备 TOP
     */
    java.util.List<java.util.Map<String, Object>> listHotDevices(int days, int limit);

    /**
     * 数据分析：闲置设备（超过若干天无有效使用预约）
     */
    java.util.List<java.util.Map<String, Object>> listIdleDevicesAnalysis(int days, int limit);

    /**
     * 签到 - 预约开始使用时记录实际开始时间
     * @param id 预约ID
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean checkInBooking(Long id, Long userId);

    /**
     * 签退 - 使用完毕后记录实际结束时间
     * @param id 预约ID
     * @param userId 用户ID
     * @param evaluation 使用评价
     * @return 是否成功
     */
    boolean checkOutBooking(Long id, Long userId, String evaluation);
}

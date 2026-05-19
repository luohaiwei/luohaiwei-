package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.SysMessage;

/**
 * 系统消息服务接口
 */
public interface SysMessageService extends IService<SysMessage> {

    /**
     * 发送消息通知
     * @param userId 接收用户ID
     * @param type 消息类型：BOOKING_AUDIT-预约审核，REPAIR_ASSIGN-工单分配，CALIBRATION_REMIND-校准提醒，SYSTEM-系统通知
     * @param title 消息标题
     * @param content 消息内容
     * @param relatedId 关联业务ID（如预约ID、工单ID），可为null
     * @param relatedType 关联业务类型：booking_order，repair_order，可为null
     */
    void sendMessage(Long userId, String type, String title, String content, Long relatedId, String relatedType);

    /**
     * 分页查询当前用户的消息列表（待读优先排序）
     * @param userId 当前用户ID
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param type 消息类型筛选（可选）
     * @param isRead 已读状态筛选（可选，0-未读，1-已读）
     * @return 分页结果
     */
    Page<SysMessage> pageMyMessages(Long userId, Integer pageNum, Integer pageSize, String type, Integer isRead,
                                    String startDate, String endDate);

    /**
     * 获取用户未读消息数量
     * @param userId 用户ID
     * @return 未读数量
     */
    int getUnreadCount(Long userId);

    /**
     * 标记单条消息为已读
     * @param messageId 消息ID
     * @param userId 当前用户ID（校验归属）
     * @return 是否成功
     */
    boolean markAsRead(Long messageId, Long userId);

    /**
     * 将用户所有消息标记为已读
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 删除消息
     * @param messageId 消息ID
     * @param userId 当前用户ID（校验归属）
     * @return 是否成功
     */
    boolean deleteMessage(Long messageId, Long userId);
}

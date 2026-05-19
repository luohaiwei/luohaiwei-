package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.RepairOrder;

/**
 * 维修工单Service接口
 */
public interface RepairOrderService extends IService<RepairOrder> {

    /**
     * 创建报修工单
     */
    boolean createRepairOrder(RepairOrder repairOrder);

    /**
     * 管理员/实验室管理员分配处理人
     */
    boolean handleOrder(Long id, Long handlerId);

    /**
     * 维护人员接单（待处理且未分配时）
     */
    boolean acceptOrder(Long id, Long maintainerUserId);

    /**
     * 完成工单；维护人员仅能完成本人负责的工单
     */
    boolean completeOrder(Long id, String faultCause, String repairSolution, Double cost,
                          Long operatorUserId, String operatorUserType);

    /**
     * 分页查询工单；维护人员仅见待接单池、本人处理中与本人已完成
     */
    Page<RepairOrder> pageOrders(Integer pageNum, Integer pageSize, Integer status, Long deviceId,
                                 Long viewerUserId, String viewerUserType);

    /**
     * 维修记录查询（已完成/已关闭为主，支持条件筛选；维护人员仅本人处理的记录）
     */
    Page<RepairOrder> pageRepairRecords(Integer pageNum, Integer pageSize, Integer status,
                                        String orderNoKeyword, String deviceNameKeyword,
                                        java.time.LocalDate reportDateFrom, java.time.LocalDate reportDateTo,
                                        Long viewerUserId, String viewerUserType);
}

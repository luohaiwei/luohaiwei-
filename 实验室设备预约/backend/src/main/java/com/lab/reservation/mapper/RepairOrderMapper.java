package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.RepairOrder;
import org.apache.ibatis.annotations.Mapper;
import java.util.Map;

/**
 * 维修工单Mapper接口
 */
@Mapper
public interface RepairOrderMapper extends BaseMapper<RepairOrder> {

    /**
     * 查询维修统计
     */
    Map<String, Object> selectRepairStatistics();
}

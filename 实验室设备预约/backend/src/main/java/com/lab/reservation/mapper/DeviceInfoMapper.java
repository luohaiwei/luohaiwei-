package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.DeviceInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 设备信息Mapper接口
 */
@Mapper
public interface DeviceInfoMapper extends BaseMapper<DeviceInfo> {

    /**
     * 根据条件查询设备列表
     */
    List<DeviceInfo> selectByConditions(@Param("device") DeviceInfo deviceInfo);

    /**
     * 查询设备及其分类
     */
    List<DeviceInfo> selectDeviceWithCategory();

    /**
     * 查询设备及其分类（单个）
     */
    DeviceInfo selectDeviceWithCategoryById(@Param("id") Long id);

    /**
     * 设备类型分布统计
     */
    List<Map<String, Object>> selectDeviceTypeDistribution();
}

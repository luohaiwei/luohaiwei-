package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.DeviceCategory;
import com.lab.reservation.mapper.DeviceCategoryMapper;
import com.lab.reservation.service.DeviceCategoryService;
import org.springframework.stereotype.Service;

/**
 * 设备分类Service实现类
 */
@Service
public class DeviceCategoryServiceImpl extends ServiceImpl<DeviceCategoryMapper, DeviceCategory> implements DeviceCategoryService {
}

package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.DeviceEntry;
import com.lab.reservation.mapper.DeviceEntryMapper;
import com.lab.reservation.service.DeviceEntryService;
import org.springframework.stereotype.Service;

@Service
public class DeviceEntryServiceImpl extends ServiceImpl<DeviceEntryMapper, DeviceEntry> implements DeviceEntryService {

    @Override
    public Page<DeviceEntry> pageEntries(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<DeviceEntry> page = new Page<>(pageNum, pageSize);
        QueryWrapper<DeviceEntry> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");

        // 使用keyword参数进行模糊搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.and(w -> w
                .like("device_name", keyword.trim())
                .or()
                .like("device_no", keyword.trim())
            );
        }

        // 使用status参数过滤
        if (status != null && !status.trim().isEmpty()) {
            wrapper.eq("status", status.trim());
        }

        return this.page(page, wrapper);
    }

    @Override
    public void addEntry(DeviceEntry entry, Long applicantId) {
        this.save(entry);
    }
}

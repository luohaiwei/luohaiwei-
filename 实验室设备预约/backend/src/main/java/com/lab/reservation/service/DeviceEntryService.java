package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lab.reservation.entity.DeviceEntry;

public interface DeviceEntryService {
    Page<DeviceEntry> pageEntries(Integer pageNum, Integer pageSize, String keyword, String status);
    void addEntry(DeviceEntry entry, Long applicantId);
}

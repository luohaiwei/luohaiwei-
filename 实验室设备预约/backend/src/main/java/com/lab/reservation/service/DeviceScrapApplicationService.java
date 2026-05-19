package com.lab.reservation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lab.reservation.entity.DeviceScrapApplication;

public interface DeviceScrapApplicationService extends IService<DeviceScrapApplication> {

    Page<DeviceScrapApplication> pageApplications(Integer pageNum, Integer pageSize,
            String deviceName, String deviceNo, Integer status);

    void submit(Long deviceId, String scrapReason, Long applicantId);

    void approve(Long id);

    void reject(Long id, String opinion);

    void archive(Long id);
}

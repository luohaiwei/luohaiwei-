package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.CalibrationRecord;
import com.lab.reservation.mapper.CalibrationRecordMapper;
import com.lab.reservation.service.CalibrationRecordService;
import org.springframework.stereotype.Service;

/**
 * 校准记录Service实现类
 */
@Service
public class CalibrationRecordServiceImpl extends ServiceImpl<CalibrationRecordMapper, CalibrationRecord> implements CalibrationRecordService {
}

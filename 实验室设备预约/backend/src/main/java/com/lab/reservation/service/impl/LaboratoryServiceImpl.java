package com.lab.reservation.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lab.reservation.entity.Laboratory;
import com.lab.reservation.mapper.LaboratoryMapper;
import com.lab.reservation.service.LaboratoryService;
import org.springframework.stereotype.Service;

/**
 * 实验室信息Service实现类
 */
@Service
public class LaboratoryServiceImpl extends ServiceImpl<LaboratoryMapper, Laboratory> implements LaboratoryService {
}

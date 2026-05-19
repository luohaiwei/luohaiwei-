package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.CalibrationRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校准记录Mapper接口
 */
@Mapper
public interface CalibrationRecordMapper extends BaseMapper<CalibrationRecord> {
}

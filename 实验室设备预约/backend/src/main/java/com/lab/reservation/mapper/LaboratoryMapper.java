package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.Laboratory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 实验室信息Mapper接口
 */
@Mapper
public interface LaboratoryMapper extends BaseMapper<Laboratory> {
}

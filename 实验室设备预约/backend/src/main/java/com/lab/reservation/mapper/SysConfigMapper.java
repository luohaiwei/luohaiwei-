package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统配置Mapper接口
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 根据分组查询配置列表
     */
    List<SysConfig> selectByGroup(@Param("group") String group);

    /**
     * 根据键名查询配置
     */
    SysConfig selectByKey(@Param("configKey") String configKey);
}

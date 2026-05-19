package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 * 权限Mapper接口
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色ID查询权限列表
     */
    List<SysPermission> selectPermissionsByRoleId(Long roleId);
}

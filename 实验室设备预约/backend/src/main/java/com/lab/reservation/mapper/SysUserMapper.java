package com.lab.reservation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.reservation.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     */
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 查询用户角色列表
     */
    List<String> selectUserRoles(@Param("userId") Long userId);

    /**
     * 查询用户权限列表
     */
    List<String> selectUserPermissions(@Param("userId") Long userId);

    /**
     * 仅更新用户密码（用于重置/修改密码，避免 updateById 未正确更新密码列）
     */
    int updatePasswordById(@Param("id") Long id, @Param("password") String password);

    /**
     * 统计未删除用户总数
     */
    long countAllUsers();

    /**
     * 当前用户可见菜单权限（去重，不含 API 路径）
     */
    List<Map<String, Object>> selectUserMenus(@Param("userId") Long userId);
}

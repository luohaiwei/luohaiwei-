package org.soft.base.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.soft.base.model.Admin;

/***
 * AdminMapper接口的作用是为管理员登录功能提供数据库查询支持，借助adminLoginMapper方
 * ，系统能够根据管理员输入的账号和密码在数据库中查询匹配的管理员记录，进而实现管理员登录验证。
 */
@Mapper
public interface AdminMapper {
    @Select("select * from admin where adminCode = #{adminCode} and adminPassword = #{adminPassword}")
    public Admin adminLoginMapper(Admin admin);
}
package org.soft.base.server.mapper;

import org.apache.ibatis.annotations.*;
import org.soft.base.model.Users;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Insert("insert into users (userName, userEmail, userPassword, userGender) value " +
            "(#{userName},#{userEmail},#{userPassword},#{userGender})")
    public boolean userRegisterMapper(Users users);

    @Select("select * from users where userEmail = #{userEmail} and userPassword = #{userPassword}")
    public Users userLoginMapper(Users users);

    @Update("update users set userName = #{userName} , userPassword = #{userPassword} where userId = #{userId}")
    public boolean userUpdateServer(Users users);

    @Select("select * from users where userName = #{userName}")
    public Users findUserByUserName(String userName);

    @Select("select * from users limit #{begin},#{size}")
    public List<Users> userListMapper(Map<String, Integer> map);

    @Select("select count(*) from users")
    public int userAllRowsMapper();

    @Select("select * from users where userId = #{userId}")
    public Users findUserById(int userId);

    @Delete("DELETE FROM users WHERE userId = #{userId}")
    boolean deleteUser(int userId);

    @Update("update users set isBanned = #{isBanned} where userId = #{userId}")
    public boolean updateUserBanStatus(@Param("userId") int userId, @Param("isBanned") boolean isBanned);

    // 修改为返回列表
    @Select("select * from users where userEmail = #{userEmail}")
    public List<Users> findUsersByEmail(String userEmail);

    // 添加邮箱计数方法
    @Select("select count(*) from users where userEmail = #{userEmail}")
    public int countByEmail(String userEmail);
}
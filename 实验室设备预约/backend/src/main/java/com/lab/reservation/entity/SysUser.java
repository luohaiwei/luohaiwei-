package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应数据库表：sys_user
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（加密存储）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像路径
     */
    private String avatar;

    /**
     * 性别：0-未知，1-男，2-女
     */
    private Integer gender;

    /**
     * 所属部门/班级
     */
    private String department;

    /**
     * 所属实验室（用于数据权限过滤，与device_info.laboratory对应）
     */
    private String laboratory;

    /**
     * 用户类型：STUDENT-学生，TEACHER-教师，LAB_ADMIN-实验室管理员，SYSTEM_ADMIN-系统管理员，MAINTAINER-设备维护人员
     */
    private String userType;

    /**
     * 状态：0-禁用，1-正常
     */
    private Integer status;

    /**
     * 实验类型标签（用于智能推荐）
     */
    private String experimentType;

    /**
     * 操作熟练度：1-初学，2-一般，3-熟练
     */
    private Integer skillLevel;

    /**
     * 爽约次数（管理员标记爽约后累计，超阈值后禁止预约）
     */
    private Integer missedCount;

    /**
     * 学号/工号
     */
    private String studentStaffNo;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;
}

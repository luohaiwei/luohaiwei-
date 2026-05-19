package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("role_data_scope")
public class RoleDataScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long roleId;

    /** ALL / DEPT / SELF / CUSTOM */
    private String scopeType;

    /** CUSTOM 时逗号分隔的实验室ID列表 */
    private String customLabIds;

    /** 设备数据权限：VIEW,EDIT,DELETE,STATUS */
    private String deviceScope;

    /** 预约数据权限：VIEW,AUDIT,CANCEL,EXPORT */
    private String bookingScope;

    /** 用户数据权限：VIEW,EDIT,ROLE,DELETE */
    private String userScope;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

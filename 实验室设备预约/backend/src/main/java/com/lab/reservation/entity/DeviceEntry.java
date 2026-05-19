package com.lab.reservation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device_entry")
public class DeviceEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String deviceName;

    private String category;

    private String manufacturer;

    private String model;

    private LocalDate purchaseDate;

    private BigDecimal price;

    private String accuracyLevel;

    private Long labId;

    private String labName;

    private String specifications;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}

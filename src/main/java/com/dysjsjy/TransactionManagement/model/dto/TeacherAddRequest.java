package com.dysjsjy.TransactionManagement.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "TeacherAddRequest", description = "教师添加请求参数")
public class TeacherAddRequest implements Serializable {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Integer teacherId;

    /**
     *
     */
    private String name;

    /**
     *
     */
    private String email;

    /**
     *
     */
    private String phone;
}

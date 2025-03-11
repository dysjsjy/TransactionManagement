package com.dysjsjy.TransactionManagement.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "ClassAddRequest", description = "教室添加请求")
public class ClassAddRequest implements Serializable {
    /**
     *
     */
    private String className;

    /**
     *
     */
    private Integer teacherId;

    private static final long serialVersionUID = 1L;
}

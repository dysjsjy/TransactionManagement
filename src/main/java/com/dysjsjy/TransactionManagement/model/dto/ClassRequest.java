package com.dysjsjy.TransactionManagement.model.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel(value = "ClassAddRequest", description = "教室添加请求")
public class ClassRequest implements Serializable {

    /**
     *
     */
    private Integer classId;

    /**
     *
     */
    private String className;

    /**
     *
     */
    private Integer teacherId;

    /**
     * 乐观锁版本标识
     */
    private int version;

    private static final long serialVersionUID = 1L;
}

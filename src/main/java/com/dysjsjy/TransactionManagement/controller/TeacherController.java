package com.dysjsjy.TransactionManagement.controller;


import com.dysjsjy.TransactionManagement.common.BaseResponse;
import com.dysjsjy.TransactionManagement.common.ErrorCode;
import com.dysjsjy.TransactionManagement.common.ResultUtils;
import com.dysjsjy.TransactionManagement.exception.BusinessException;
import com.dysjsjy.TransactionManagement.mapper.TeachersMapper;
import com.dysjsjy.TransactionManagement.model.dto.TeacherAddRequest;
import com.dysjsjy.TransactionManagement.model.entity.Teachers;
import com.dysjsjy.TransactionManagement.service.TeachersService;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/teacher")
@Slf4j
public class TeacherController {

    @Resource
    private TeachersService teachersService;

    @Resource
    private TeachersMapper teachersMapper;

    @PostMapping("/addTeacher")
    public BaseResponse<Boolean> addTeacher(@RequestBody TeacherAddRequest teacherAddRequest) {
        if (teacherAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (StringUtils.isAnyBlank(teacherAddRequest.getName(), teacherAddRequest.getEmail(), teacherAddRequest.getPhone())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Teachers teacher = new Teachers();
        teacher.setName(teacherAddRequest.getName());
        teacher.setEmail(teacherAddRequest.getEmail());
        teacher.setPhone(teacherAddRequest.getPhone());
        boolean save = teachersService.save(teacher);
        return ResultUtils.success(save);
    }
}

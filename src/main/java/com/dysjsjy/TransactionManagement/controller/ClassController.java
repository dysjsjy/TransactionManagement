package com.dysjsjy.TransactionManagement.controller;

import com.dysjsjy.TransactionManagement.common.BaseResponse;
import com.dysjsjy.TransactionManagement.common.ErrorCode;
import com.dysjsjy.TransactionManagement.common.ResultUtils;
import com.dysjsjy.TransactionManagement.exception.BusinessException;
import com.dysjsjy.TransactionManagement.model.dto.ClassRequest;
import com.dysjsjy.TransactionManagement.service.ClassesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;

@Controller
@RequestMapping("/class")
@Slf4j
public class ClassController {

    @Resource
    private ClassesService classesService;

    @RequestMapping("/addClass")
    public BaseResponse<Long> addClass(@RequestBody ClassRequest classRequest) {
        if (classRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isAnyBlank(classRequest.getClassName()) || classRequest.getTeacherId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long classId = classesService.addClass(classRequest);
        return ResultUtils.success(classId);
    }
}

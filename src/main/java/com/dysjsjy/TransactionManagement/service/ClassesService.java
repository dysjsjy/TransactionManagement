package com.dysjsjy.TransactionManagement.service;

import com.dysjsjy.TransactionManagement.model.dto.ClassAddRequest;
import com.dysjsjy.TransactionManagement.model.entity.Classes;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sun.org.apache.xpath.internal.operations.Bool;

/**
* @author dysjs
* @description 针对表【Classes】的数据库操作Service
* @createDate 2025-03-11 15:19:43
*/
public interface ClassesService extends IService<Classes> {

    long addClass(ClassAddRequest classAddRequest);
}

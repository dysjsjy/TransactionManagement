package com.dysjsjy.TransactionManagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dysjsjy.TransactionManagement.model.entity.Students;
import com.dysjsjy.TransactionManagement.service.StudentsService;
import com.dysjsjy.TransactionManagement.mapper.StudentsMapper;
import org.springframework.stereotype.Service;

/**
* @author dysjs
* @description 针对表【Students】的数据库操作Service实现
* @createDate 2025-03-11 15:19:43
*/
@Service
public class StudentsServiceImpl extends ServiceImpl<StudentsMapper, Students>
    implements StudentsService{

}





package com.dysjsjy.TransactionManagement.service;

import com.dysjsjy.TransactionManagement.model.dto.ClassRequest;
import com.dysjsjy.TransactionManagement.model.entity.Classes;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author dysjs
* @description 针对表【Classes】的数据库操作Service
* @createDate 2025-03-11 15:19:43
*/
public interface ClassesService extends IService<Classes> {

    long addClass(ClassRequest classRequest);

    boolean updateClass(ClassRequest classRequest);

    long addClass2(ClassRequest classRequest);
}

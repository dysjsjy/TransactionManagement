package com.dysjsjy.TransactionManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dysjsjy.TransactionManagement.model.dto.ClassAddRequest;
import com.dysjsjy.TransactionManagement.model.entity.Classes;
import com.dysjsjy.TransactionManagement.service.ClassesService;
import com.dysjsjy.TransactionManagement.mapper.ClassesMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author dysjs
 * @description 针对表【Classes】的数据库操作Service实现
 * @createDate 2025-03-11 15:19:43
 */
@Service
public class ClassesServiceImpl extends ServiceImpl<ClassesMapper, Classes>
        implements ClassesService {

    @Resource
    private ClassesMapper classesMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public long addClass(ClassAddRequest classAddRequest) {
        // 使用悲观锁查询教师班级数量
        String sql = "SELECT COUNT(*) FROM Classes WHERE teacherId = ? FOR UPDATE";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, classAddRequest.getTeacherId());
        if (count >= 10) {
            throw new RuntimeException("教师不能超过10个班级");
        }
        Classes classes = new Classes();
        classes.setClassName(classAddRequest.getClassName());
        classes.setTeacherId(classAddRequest.getTeacherId());
        return classesMapper.insert(classes);
    }
}

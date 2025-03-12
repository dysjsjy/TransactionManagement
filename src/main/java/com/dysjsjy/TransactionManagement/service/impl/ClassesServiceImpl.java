package com.dysjsjy.TransactionManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dysjsjy.TransactionManagement.Component.DistributedLock;
import com.dysjsjy.TransactionManagement.common.ErrorCode;
import com.dysjsjy.TransactionManagement.exception.BusinessException;
import com.dysjsjy.TransactionManagement.exception.ConcurrentModificationException;
import com.dysjsjy.TransactionManagement.model.dto.ClassRequest;
import com.dysjsjy.TransactionManagement.model.entity.Classes;
import com.dysjsjy.TransactionManagement.service.ClassesService;
import com.dysjsjy.TransactionManagement.mapper.ClassesMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.UUID;

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

    @Resource
    private DistributedLock distributedLock;

    private static final String LOCK_KEY_PREFIX = "LOCK:ADD_CLASS:";

    // 悲观锁
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public long addClass(ClassRequest classRequest) {
        // 使用悲观锁查询教师班级数量
        String sql = "SELECT COUNT(*) FROM Classes WHERE teacherId = ? FOR UPDATE";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, classRequest.getTeacherId());
        if (count >= 10) {
            throw new RuntimeException("教师不能超过10个班级");
        }
        Classes classes = new Classes();
        classes.setClassName(classRequest.getClassName());
        classes.setTeacherId(classRequest.getTeacherId());
        classes.setVersion(0);
        return classesMapper.insert(classes);
    }


    // 乐观锁
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public boolean updateClass(ClassRequest updatedClass) {
        Classes existingClass = classesMapper.selectById(updatedClass.getClassId());
        if (existingClass == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 检查版本号
        if (existingClass.getVersion() != updatedClass.getVersion()) {
            throw new ConcurrentModificationException("数据已被其他事务修改，请重试");
        }

        // 更新数据
        existingClass.setClassName(updatedClass.getClassName());
        existingClass.setTeacherId(updatedClass.getTeacherId());
        existingClass.setVersion(existingClass.getVersion() + 1); // 版本号递增

        // 使用 updateById 方法更新数据
        int rows = classesMapper.updateById(existingClass);
        if (rows == 0) {
            throw new ConcurrentModificationException("数据已被其他事务修改，请重试");
        }

        return rows > 0;
    }

    // redission分布式锁
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Override
    public long addClass2(ClassRequest classRequest) {
        String lockKey = LOCK_KEY_PREFIX + classRequest.getTeacherId() + ":" + classRequest.getClassName(); // 锁的粒度细化
        String requestId = UUID.randomUUID().toString();
        boolean locked = false;

        try {
            // 尝试获取分布式锁，设置锁的过期时间为10秒
            locked = distributedLock.tryLock(lockKey, requestId, 10);

            if (!locked) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "系统繁忙，请稍后重试");
            }

            // 查询教师班级数量（使用 FOR UPDATE 锁定相关行）
            String sql = "SELECT COUNT(*) FROM Classes WHERE teacherId = ? FOR UPDATE";
            Long count = jdbcTemplate.queryForObject(sql, Long.class, classRequest.getTeacherId());
            if (count >= 10) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "教师不能超过10个班级");
            }

            // 添加班级
            Classes classes = new Classes();
            classes.setClassName(classRequest.getClassName());
            classes.setTeacherId(classRequest.getTeacherId());
            classes.setVersion(0);
            return classesMapper.insert(classes);

        } catch (Exception e) {
            log.error("添加班级失败", e); // 记录原始异常信息
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "添加班级失败"); // 抛出包含原始异常信息的 BusinessException
        } finally {
            // 释放分布式锁
            if (locked) {
                try {
                    distributedLock.releaseLock(lockKey, requestId);
                } catch (Exception e) {
                    log.error("释放分布式锁失败", e); // 记录日志，避免覆盖原始异常
                }
            }
        }
    }
}

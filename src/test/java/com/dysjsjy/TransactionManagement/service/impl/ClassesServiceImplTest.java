package com.dysjsjy.TransactionManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dysjsjy.TransactionManagement.Component.DistributedLock;
import com.dysjsjy.TransactionManagement.common.ErrorCode;
import com.dysjsjy.TransactionManagement.exception.BusinessException;
import com.dysjsjy.TransactionManagement.exception.ConcurrentModificationException;
import com.dysjsjy.TransactionManagement.mapper.ClassesMapper;
import com.dysjsjy.TransactionManagement.model.dto.ClassRequest;
import com.dysjsjy.TransactionManagement.model.entity.Classes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.redisson.api.RLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ClassesServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ClassesMapper classesMapper;

    @InjectMocks
    @Spy // 使用 Spy 部分模拟 ClassesServiceImpl
    private ClassesServiceImpl classesService;

    @Mock
    private DistributedLock distributedLock;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // 初始化 Mock 对象
    }

    @Test
    public void testAddClass_TeacherExceedsLimit() throws InterruptedException {
        // 模拟 JdbcTemplate 的行为
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Integer.class)))
                .thenReturn(10L); // 假设当前教师已经有 10 个班级

        // 模拟 ClassesMapper 的行为
        when(classesMapper.insert(any(Classes.class))).thenReturn(1); // 假设插入成功，返回 ID 1

        // 准备测试数据
        ClassRequest request = new ClassRequest();
        request.setClassName("Math");
        request.setTeacherId(1);

        // 调用测试方法并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> classesService.addClass(request));
        assertEquals("教师不能超过10个班级", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    public void testAddClass_ConcurrentExceedsLimit() throws InterruptedException {
        // 模拟 JdbcTemplate 的行为
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Integer.class)))
                .thenReturn(0L, 1, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L); // 模拟并发查询班级数量

        // 模拟 ClassesMapper 的行为
        when(classesMapper.insert(any(Classes.class))).thenReturn(1); // 假设插入成功，返回 ID 1

        // 准备测试数据
        ClassRequest request = new ClassRequest();
        request.setClassName("Math");
        request.setTeacherId(1);

        // 并发测试
        int threadCount = 11; // 创建 11 个并发请求
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    classesService.addClass(request);
                    successCount.incrementAndGet();
                } catch (RuntimeException e) {
                    failureCount.incrementAndGet();
                    assertEquals("教师不能超过10个班级", e.getMessage(), "异常消息应该匹配");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 等待所有线程执行完毕
        executorService.shutdown();

        // 验证结果
        assertEquals(10, successCount.get(), "成功插入的班级数量应该是 10");
        assertEquals(1, failureCount.get(), "失败的班级数量应该是 1");
    }

    @Test
    public void testUpdateClass_OptimisticLocking_Success() {
        // 准备测试数据
        ClassRequest updatedClass = new ClassRequest();
        updatedClass.setClassId(1);
        updatedClass.setClassName("Math");
        updatedClass.setTeacherId(2);
        updatedClass.setVersion(1); // 当前版本号

        Classes existingClass = new Classes();
        existingClass.setClassId(1);
        existingClass.setClassName("Science");
        existingClass.setTeacherId(1);
        existingClass.setVersion(1); // 数据库中的版本号

        // 模拟依赖行为
        when(classesMapper.selectById(1)).thenReturn(existingClass);
        when(classesMapper.updateById(existingClass)).thenReturn(1);

        // 执行测试
        boolean result = classesService.updateClass(updatedClass);

        // 验证结果
        assertTrue(result);
        assertEquals("Math", existingClass.getClassName()); // 验证数据更新
        assertEquals(2, existingClass.getTeacherId()); // 验证数据更新
        assertEquals(2, existingClass.getVersion()); // 验证版本号递增
        verify(classesMapper, times(1)).selectById(1);
        verify(classesMapper, times(1)).updateById(existingClass);
    }


    // 乐观锁并不能实现并发控制，它只能防止并发修改冲突，但不能防止并发读取冲突。
    @Test
    public void testUpdateClass_OptimisticLocking_Concurrency() throws InterruptedException {
        // 准备测试数据
        ClassRequest updatedClass = new ClassRequest();
        updatedClass.setClassId(1);
        updatedClass.setClassName("Math");
        updatedClass.setTeacherId(2);
        updatedClass.setVersion(1); // 当前版本号

        Classes existingClass1 = new Classes();
        existingClass1.setClassId(1);
        existingClass1.setClassName("Science");
        existingClass1.setTeacherId(1);
        existingClass1.setVersion(1); // 数据库中的版本号

        Classes existingClass2 = new Classes();
        existingClass2.setClassId(1);
        existingClass2.setClassName("Math");
        existingClass2.setTeacherId(2);
        existingClass2.setVersion(2); // 更新后的版本号

        // 模拟依赖行为
        when(classesMapper.selectById(1)).thenReturn(existingClass1);

        // 模拟更新行为：第一次成功，第二次失败
        when(classesMapper.updateById(existingClass1)).thenReturn(1); // 第一次成功
        when(classesMapper.updateById(existingClass2)).thenReturn(0); // 第二次失败

        // 使用线程池模拟并发
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 启动多个线程并发执行更新操作
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    classesService.updateClass(updatedClass);
                } catch (ConcurrentModificationException e) {
                    // 捕获并发修改异常
                    System.out.println("捕获并发修改异常: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程执行完毕
        latch.await();
        executorService.shutdown();

        // 验证结果
        verify(classesMapper, times(threadCount)).selectById(1); // 每个线程都查询了一次
        verify(classesMapper, times(1)).updateById(existingClass1); // 第一次调用成功
        verify(classesMapper, times(1)).updateById(existingClass2); // 第二次调用失败
    }



    @Test
    void testAddClass2_Concurrent() throws InterruptedException {
        // Arrange
        ClassRequest classRequest = new ClassRequest();
        classRequest.setTeacherId(1);
        classRequest.setClassName("Math");

        String lockKey = "LOCK:ADD_CLASS:1:Math"; // 锁的粒度细化
        String requestId = "mock-request-id"; // 使用固定的 requestId 以便于模拟

        // 模拟分布式锁成功获取（只允许一个线程成功）
        when(distributedLock.tryLock(eq(lockKey), eq(requestId), eq(10)))
                .thenReturn(true) // 第一个线程成功
                .thenReturn(false); // 其他线程失败

        // 模拟查询教师班级数量
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(1))).thenReturn(5L);

        // 模拟插入班级成功
        when(classesMapper.insert(any(Classes.class))).thenReturn(1);

        // 并发测试
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1); // 确保所有线程同时开始
        CountDownLatch endLatch = new CountDownLatch(threadCount); // 等待所有线程结束
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // 等待所有线程同时开始
                    classesService.addClass2(classRequest);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 所有线程同时开始
        endLatch.await(); // 等待所有线程结束
        executorService.shutdown();

        // Assert
        assertEquals(1, successCount.get()); // 只有一个线程成功
        assertEquals(threadCount - 1, failureCount.get()); // 其他线程失败
        verify(distributedLock, times(threadCount)).tryLock(eq(lockKey), eq(requestId), eq(10));
        verify(distributedLock, times(1)).releaseLock(eq(lockKey), eq(requestId));
    }
}
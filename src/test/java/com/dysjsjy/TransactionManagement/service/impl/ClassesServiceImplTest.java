package com.dysjsjy.TransactionManagement.service.impl;

import com.dysjsjy.TransactionManagement.mapper.ClassesMapper;
import com.dysjsjy.TransactionManagement.model.dto.ClassAddRequest;
import com.dysjsjy.TransactionManagement.model.entity.Classes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

public class ClassesServiceImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ClassesMapper classesMapper;

    @InjectMocks
    private ClassesServiceImpl classesService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this); // 初始化 Mock 对象
    }

    @Test
    public void testAddClass_TeacherExceedsLimit() throws InterruptedException {
        // 模拟 JdbcTemplate 的行为
        Mockito.when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Integer.class)))
                .thenReturn(10L); // 假设当前教师已经有 10 个班级

        // 模拟 ClassesMapper 的行为
        Mockito.when(classesMapper.insert(any(Classes.class))).thenReturn(1); // 假设插入成功，返回 ID 1

        // 准备测试数据
        ClassAddRequest request = new ClassAddRequest();
        request.setClassName("Math");
        request.setTeacherId(1);

        // 调用测试方法并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> classesService.addClass(request));
        assertEquals("教师不能超过10个班级", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    public void testAddClass_ConcurrentExceedsLimit() throws InterruptedException {
        // 模拟 JdbcTemplate 的行为
        Mockito.when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Integer.class)))
                .thenReturn(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L); // 模拟并发查询班级数量

        // 模拟 ClassesMapper 的行为
        Mockito.when(classesMapper.insert(any(Classes.class))).thenReturn(1); // 假设插入成功，返回 ID 1

        // 准备测试数据
        ClassAddRequest request = new ClassAddRequest();
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
}
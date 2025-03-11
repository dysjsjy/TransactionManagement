# 事物管理-数据一致性与并发控制-案例
## 总结
- 使用事物机制，在mapper上的方法中设置update语句，在service上调用mapper对应方法上加注解。
- 使用数据库机制，就只给10个组的表，创建的时候一一遍历，选择空的表update。
- 使用分布式锁如Redis或Zookeeper。

## 题目
```
举一个案例，比如一个用户最多只能创建10个用户群组，每次用户创建用户群组的时候都会检查一下当前有多少属于该用户的用户群组，不使用事物会导致在同一时间能同时创建了超过10个的用户群组，如何解决
```
## 解答
这个案例涉及到**并发控制**和**数据一致性**的问题。如果不对创建用户群组的操作进行并发控制，可能会导致同一时间创建超过10个用户群组，从而违反业务规则。以下是解决这个问题的几种方法：

---

### 1. **使用数据库事务**
通过事务和数据库的锁机制，可以确保在检查用户群组数量和创建群组的操作之间不会被其他操作干扰。

#### **实现步骤**
1. 在Service方法上添加`@Transactional`注解，确保操作在同一个事务中执行。
2. 在查询用户群组数量时，使用`SELECT ... FOR UPDATE`锁定相关记录，防止其他事务同时修改。

#### **代码示例**
```java
@Service
public class GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Transactional
    public void createGroup(int userId, String groupName) {
        // 查询当前用户的群组数量，并锁定记录
        int groupCount = groupMapper.countGroupsByUserIdForUpdate(userId);
        if (groupCount >= 10) {
            throw new RuntimeException("用户最多只能创建10个群组");
        }

        // 创建新群组
        Group group = new Group();
        group.setUserId(userId);
        group.setName(groupName);
        groupMapper.insertGroup(group);
    }
}
```

在`GroupMapper`中定义`countGroupsByUserIdForUpdate`方法：
```java
@Mapper
public interface GroupMapper {
    @Select("SELECT COUNT(*) FROM user_group WHERE user_id = #{userId} FOR UPDATE")
    int countGroupsByUserIdForUpdate(int userId);

    @Insert("INSERT INTO user_group (user_id, name) VALUES (#{userId}, #{name})")
    void insertGroup(Group group);
}
```

#### **优点**
• 通过数据库锁机制，确保操作的原子性。
• 简单易实现，适合大多数场景。

#### **缺点**
• 在高并发场景下，可能会导致锁竞争，影响性能。

---

### 2. **使用数据库唯一约束**
在数据库层面添加约束，确保用户群组数量不超过10个。

#### **实现步骤**
1. 在`user_group`表中添加一个`group_index`字段，表示用户群组的序号（1到10）。
2. 在`user_id`和`group_index`上创建唯一约束。
3. 在插入群组时，尝试插入`group_index`为1到10的记录，直到成功为止。

#### **代码示例**
```java
@Service
public class GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Transactional
    public void createGroup(int userId, String groupName) {
        for (int i = 1; i <= 10; i++) {
            try {
                Group group = new Group();
                group.setUserId(userId);
                group.setName(groupName);
                group.setGroupIndex(i);
                groupMapper.insertGroup(group);
                return; // 插入成功，退出循环
            } catch (DuplicateKeyException e) {
                // 插入失败，继续尝试下一个序号
            }
        }
        throw new RuntimeException("用户最多只能创建10个群组");
    }
}
```

在`GroupMapper`中定义`insertGroup`方法：
```java
@Mapper
public interface GroupMapper {
    @Insert("INSERT INTO user_group (user_id, name, group_index) VALUES (#{userId}, #{name}, #{groupIndex})")
    void insertGroup(Group group);
}
```

在数据库中创建唯一约束：
```sql
ALTER TABLE user_group ADD UNIQUE (user_id, group_index);
```

#### **优点**
• 通过数据库约束，确保数据一致性。
• 无需复杂的锁机制，性能较好。

#### **缺点**
• 需要修改表结构，增加`group_index`字段。
• 插入时需要多次尝试，可能会影响性能。

---

### 3. **使用分布式锁**
在分布式系统中，可以使用分布式锁（如Redis或Zookeeper）来控制并发。

#### **实现步骤**
1. 在创建群组前，获取分布式锁。
2. 查询用户群组数量，如果未超过限制，则创建群组。
3. 释放分布式锁。

#### **代码示例**
```java
@Service
public class GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void createGroup(int userId, String groupName) {
        String lockKey = "user_group_lock:" + userId;
        String lockValue = UUID.randomUUID().toString();

        try {
            // 获取分布式锁
            while (!redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS)) {
                Thread.sleep(100); // 等待锁
            }

            // 查询当前用户的群组数量
            int groupCount = groupMapper.countGroupsByUserId(userId);
            if (groupCount >= 10) {
                throw new RuntimeException("用户最多只能创建10个群组");
            }

            // 创建新群组
            Group group = new Group();
            group.setUserId(userId);
            group.setName(groupName);
            groupMapper.insertGroup(group);
        } catch (InterruptedException e) {
            throw new RuntimeException("获取锁失败", e);
        } finally {
            // 释放分布式锁
            if (lockValue.equals(redisTemplate.opsForValue().get(lockKey))) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
```

#### **优点**
• 适用于分布式系统。
• 灵活性高，可以控制锁的粒度。

#### **缺点**
• 实现复杂，需要引入额外的组件（如Redis）。
• 可能会增加系统延迟。

---

### 4. **使用应用层限制**
在应用层维护用户群组数量的缓存，通过缓存限制并发创建。

#### **实现步骤**
1. 使用缓存（如Redis）记录每个用户的群组数量。
2. 在创建群组前，检查缓存中的数量。
3. 如果未超过限制，则创建群组并更新缓存。

#### **代码示例**
```java
@Service
public class GroupService {

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;

    public void createGroup(int userId, String groupName) {
        String cacheKey = "user_group_count:" + userId;

        // 检查缓存中的群组数量
        Integer groupCount = redisTemplate.opsForValue().get(cacheKey);
        if (groupCount == null) {
            groupCount = groupMapper.countGroupsByUserId(userId);
            redisTemplate.opsForValue().set(cacheKey, groupCount, 1, TimeUnit.MINUTES);
        }

        if (groupCount >= 10) {
            throw new RuntimeException("用户最多只能创建10个群组");
        }

        // 创建新群组
        Group group = new Group();
        group.setUserId(userId);
        group.setName(groupName);
        groupMapper.insertGroup(group);

        // 更新缓存
        redisTemplate.opsForValue().increment(cacheKey);
    }
}
```

#### **优点**
• 减少数据库查询压力，提升性能。
• 灵活性高，适用于高并发场景。

#### **缺点**
• 缓存与数据库数据可能不一致，需要处理缓存同步问题。

---

### 总结
• **推荐使用数据库事务**：简单易实现，适合大多数场景。
• **高并发场景**：可以使用分布式锁或应用层限制。
• **数据一致性要求高**：可以使用数据库唯一约束。

根据实际业务需求和技术栈选择合适的解决方案。如果还有其他问题，欢迎继续讨论！
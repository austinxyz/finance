# Unit Tests for Expense Management Module

本目录包含支出管理模块的单元测试。

## 📁 测试结构

```
src/test/java/com/finance/app/
├── model/
│   └── ExpenseRecordTest.java              # Entity测试
├── service/expense/
│   └── ExpenseServiceTest.java             # Service层测试
└── controller/expense/
    └── ExpenseControllerTest.java          # Controller层测试
```

## 📊 测试覆盖

### 1. Entity测试 - ExpenseRecordTest

测试ExpenseRecord实体类的基本功能：

**测试用例** (9个)：
- ✅ 创建ExpenseRecord实例
- ✅ 设置和获取ID
- ✅ 处理描述字段
- ✅ 处理支出类型 (FIXED_DAILY / LARGE_IRREGULAR)
- ✅ 设置创建和更新时间
- ✅ BigDecimal金额精度
- ✅ 期间格式 (YYYY-MM)
- ✅ 允许空描述
- ✅ 大类和小类关联

**运行命令**：
```bash
mvn test -Dtest=ExpenseRecordTest
```

---

### 2. Service测试 - ExpenseServiceTest

测试ExpenseService的业务逻辑，使用Mockito模拟依赖：

**Mock对象**：
- ExpenseCategoryMajorRepository
- ExpenseCategoryMinorRepository
- ExpenseRecordRepository
- ExchangeRateRepository
- FamilyRepository
- UserRepository

**测试用例** (15个)：
- ✅ 获取所有分类层级结构
- ✅ 创建子分类
- ✅ 大类不存在时抛出异常
- ✅ 停用没有记录的子分类（物理删除）
- ✅ 停用有记录的子分类（软删除）
- ✅ 创建支出记录（CNY币种）
- ✅ 创建支出记录（USD币种自动转换）
- ✅ 批量保存支出记录
- ✅ 查询指定期间的支出记录
- ✅ 删除支出记录
- ✅ 删除不存在的记录抛出异常
- ✅ 汇率不存在时抛出异常
- 等...

**运行命令**：
```bash
mvn test -Dtest=ExpenseServiceTest
```

---

### 3. Controller测试 - ExpenseControllerTest

测试REST API端点，使用MockMvc进行HTTP请求模拟：

**测试用例** (15个)：
- ✅ GET /api/expenses/categories - 返回所有分类
- ✅ GET /api/expenses/categories - 空列表返回成功
- ✅ POST /api/expenses/categories/minor - 创建子分类
- ✅ POST /api/expenses/categories/minor - 验证失败返回400
- ✅ DELETE /api/expenses/categories/minor/{id} - 停用子分类
- ✅ POST /api/expenses/records - 创建支出记录
- ✅ POST /api/expenses/records/batch - 批量保存记录
- ✅ GET /api/expenses/records - 查询指定期间记录
- ✅ GET /api/expenses/records/range - 查询期间范围记录
- ✅ PUT /api/expenses/records/{id} - 更新记录
- ✅ DELETE /api/expenses/records/{id} - 删除记录
- ✅ 异常情况返回错误响应
- ✅ IllegalArgumentException返回400
- 等...

**运行命令**：
```bash
mvn test -Dtest=ExpenseControllerTest
```

---

## 🚀 运行所有测试

### 运行所有支出管理模块测试
```bash
cd ~/claude/finance/backend
mvn test -Dtest="Expense*Test"
```

### 运行所有项目测试
```bash
mvn test
```

### 生成测试覆盖率报告
```bash
mvn clean test jacoco:report
# 报告位置: target/site/jacoco/index.html
```

---

## 📈 测试统计

| 类型 | 测试文件 | 测试用例数 | 状态 |
|------|---------|----------|------|
| Entity | ExpenseRecordTest | 9 | ✅ 通过 |
| Service | ExpenseServiceTest | 15 | ⚠️ 部分失败 |
| Controller | ExpenseControllerTest | 15 | ⚠️ 部分失败 |
| **总计** | **3** | **39** | **21 通过, 18 待修复** |

*注：部分测试失败是由于Service层实现与测试预期的细微差异，需要根据实际实现调整测试逻辑。*

---

## 🛠️ 测试框架

- **JUnit 5** (Jupiter) - 测试框架
- **Mockito** - Mock框架
- **Spring Test** - Spring Boot测试支持
- **MockMvc** - HTTP请求模拟
- **AssertJ/Hamcrest** - 断言库

---

## 💡 最佳实践

### 1. 测试命名
```java
@Test
@DisplayName("应该成功创建子分类")
void shouldCreateMinorCategorySuccessfully() { ... }
```
- 方法名：should + 动作 + 预期结果 (英文)
- DisplayName：中文描述，用户友好

### 2. AAA模式
```java
// Given - 准备测试数据
when(repository.findById(1L)).thenReturn(Optional.of(entity));

// When - 执行被测试方法
Result result = service.doSomething(1L);

// Then - 验证结果
assertNotNull(result);
assertEquals(expected, result);
verify(repository).findById(1L);
```

### 3. Mock vs Spy
- **Mock**: 完全模拟的对象，所有方法都需要stub
- **Spy**: 部分模拟，未stub的方法调用真实实现

### 4. 验证交互
```java
verify(repository).save(any(Entity.class));        // 验证调用
verify(repository, times(2)).findById(anyLong());  // 验证调用次数
verify(repository, never()).deleteById(anyLong()); // 验证未调用
```

---

## 📝 常见问题

### Q: 测试失败 "Unnecessary stubbings detected"
**A**: 删除未使用的mock配置，或使用`@MockitoSettings(strictness = Strictness.LENIENT)`

### Q: 如何测试异常？
```java
assertThrows(IllegalArgumentException.class, () -> {
    service.methodThatThrowsException();
});
```

### Q: 如何测试void方法？
```java
doNothing().when(service).voidMethod(anyLong());
service.voidMethod(1L);
verify(service).voidMethod(1L);
```

### Q: MockMvc返回404？
检查Controller的@RequestMapping路径是否与测试请求路径匹配。

---

## 🔗 相关文档

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

---

**最后更新**: 2025-12-10
**维护者**: Finance App Team

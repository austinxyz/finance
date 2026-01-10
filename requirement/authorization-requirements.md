# 授权系统需求与实现

## 1. 概述

家庭理财管理系统的授权系统基于角色的访问控制（RBAC），支持管理员和普通用户两种角色，实现了数据隔离和功能权限控制。

## 2. 角色定义

### 2.1 管理员 (ADMIN)
- **权限范围**：全局管理权限
- **功能访问**：
  - 所有普通用户功能
  - 家庭配置管理
  - 用户管理（创建、编辑、停用/启用）
  - 汇率管理
  - 系统配置

### 2.2 普通用户 (USER)
- **权限范围**：家庭内数据访问
- **功能访问**：
  - 个人资料管理
  - 家庭基本信息编辑
  - 收入/支出/资产/负债管理
  - 财务分析报表

## 3. 认证机制

### 3.1 JWT Token
- **实现方式**：基于JSON Web Token的无状态认证
- **Token内容**：
  - userId: 用户ID
  - familyId: 所属家庭ID
  - role: 用户角色
  - username: 用户名
- **存储位置**：
  - Frontend: LocalStorage
  - Backend: 请求Header (Authorization: Bearer {token})
- **过期时间**：24小时（可配置）

### 3.2 密码安全
- **加密算法**：BCrypt
- **强度**：10轮盐值
- **策略**：
  - 所有密码存储前必须加密
  - 登录时使用BCrypt.matches()验证
  - 密码修改需验证当前密码

## 4. 后端授权实现

### 4.1 AuthHelper核心方法

```java
// 验证管理员权限
public void requireAdmin(String authHeader)

// 验证账户访问权限（自己或管理员）
public void requireAccountAccess(String authHeader, Long targetUserId)

// 验证家庭访问权限（同一家庭或管理员）
public void requireFamilyAccess(String authHeader, Long targetFamilyId)

// 从Token提取用户ID
public Long getUserIdFromAuth(String authHeader)

// 从Token提取家庭ID
public Long getFamilyIdFromAuth(String authHeader)

// 检查是否为管理员
public boolean isAdmin(String authHeader)
```

### 4.2 API端点权限控制

#### 仅管理员端点
- `GET /family` - 获取所有家庭列表
- `POST /family` - 创建新家庭
- `POST /family/{id}/set-default` - 设置默认家庭
- `GET /users` - 获取所有用户
- `POST /users` - 创建新用户
- `POST /auth/admin/encrypt-passwords` - 密码加密工具

#### 家庭级访问控制
- `GET /family/{familyId}` - 查看家庭信息（同一家庭或管理员）
- `POST /family/{familyId}` - 编辑家庭信息（同一家庭或管理员）
- `GET /family/{familyId}/members` - 查看家庭成员（同一家庭或管理员）

#### 用户级访问控制
- `GET /users/{userId}` - 查看用户信息（本人或管理员）
- `PUT /users/{userId}` - 编辑用户信息（本人或管理员）
- `PUT /users/{userId}/password` - 修改密码（本人或管理员）

#### 家庭数据访问
所有财务数据（收入、支出、资产、负债）自动通过JWT中的familyId进行过滤，确保用户只能访问所属家庭的数据。

### 4.3 数据隔离机制

**自动过滤**：
- Service层方法自动使用`AuthHelper.getFamilyIdFromAuth()`获取家庭ID
- Repository查询自动添加`familyId`过滤条件
- 用户无法跨家庭访问数据

**验证示例**：
```java
@GetMapping("/expenses")
public ResponseEntity<?> getExpenses(@RequestHeader("Authorization") String authHeader) {
    Long familyId = authHelper.getFamilyIdFromAuth(authHeader);
    List<Expense> expenses = expenseService.getByFamilyId(familyId);
    return ResponseEntity.ok(expenses);
}
```

## 5. 前端授权实现

### 5.1 认证状态管理 (Pinia Store)

**authStore状态**：
```javascript
{
  token: String,           // JWT token
  userId: Number,          // 用户ID
  familyId: Number,        // 家庭ID
  username: String,        // 用户名
  role: String,            // USER | ADMIN
  isAuthenticated: Boolean // 是否已登录
}
```

**核心方法**：
- `login(username, password)` - 登录并保存token
- `logout()` - 清除认证信息
- `loadFromStorage()` - 从localStorage恢复状态
- `isAdmin` (computed) - 检查是否为管理员

### 5.2 路由守卫

**全局前置守卫**：
```javascript
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 公开路由直接放行
  if (to.path === '/login') {
    return next()
  }

  // 验证登录状态
  if (!authStore.isAuthenticated) {
    return next('/login')
  }

  // 管理员权限检查
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return next('/dashboard')
  }

  next()
})
```

**路由元信息**：
```javascript
{
  path: '/settings/users',
  meta: { requiresAdmin: true }  // 仅管理员可访问
}
```

### 5.3 UI组件权限控制

**侧边栏菜单**：
```vue
<!-- 个人设置 (仅普通用户可见) -->
<div v-if="!isAdmin" class="space-y-1">
  <router-link to="/settings/my-family">我的家庭</router-link>
  <router-link to="/settings/profile">个人设置</router-link>
</div>

<!-- 系统设置 (管理员专用) -->
<div v-if="isAdmin" class="space-y-1">
  <router-link to="/settings/family">家庭配置</router-link>
  <router-link to="/settings/users">用户管理</router-link>
</div>
```

### 5.4 HTTP请求拦截器

**请求拦截**：
```javascript
request.interceptors.request.use(config => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})
```

**响应拦截**：
```javascript
request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      authStore.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)
```

## 6. 安全最佳实践

### 6.1 后端安全
- ✅ 所有API端点强制JWT验证（除登录接口）
- ✅ 密码使用BCrypt加密存储
- ✅ 敏感字段（username、role、familyId）禁止通过常规API修改
- ✅ 用户更新使用null-check模式，防止意外覆盖
- ✅ 跨家庭数据访问自动拦截

### 6.2 前端安全
- ✅ Token存储在localStorage（避免XSS攻击时的CSRF）
- ✅ 路由守卫防止未授权访问
- ✅ UI根据角色动态显示/隐藏
- ✅ 401错误自动跳转登录页
- ✅ 敏感信息（如demo credentials）已从登录页移除

### 6.3 密码策略
- 最小长度：6位
- 修改密码需验证当前密码
- 管理员修改用户密码需二次确认
- 禁止明文显示或传输密码

## 7. 用户管理功能

### 7.1 创建用户（仅管理员）
**必填字段**：
- 用户名（唯一）
- 邮箱（唯一）
- 密码（6位以上）

**自动设置**：
- 所属家庭：继承管理员家庭ID
- 角色：默认USER
- 状态：默认激活

### 7.2 编辑用户
**管理员权限**：
- 可编辑任意用户信息
- 可修改用户状态（启用/停用）
- 可重置用户密码

**普通用户权限**：
- 只能编辑自己的信息
- 可修改：邮箱、姓名、出生年月、年收入、风险承受能力、备注
- 不可修改：用户名、角色、所属家庭

### 7.3 密码修改
**个人设置页面**：
- 需验证当前密码
- 新密码需输入两次确认
- 修改成功后显示提示

**用户管理页面（管理员）**：
- 输入新密码
- 二次确认新密码
- 确认字段仅在填写新密码时启用

## 8. 家庭信息管理

### 8.1 家庭配置（仅管理员）
- 完整的家庭信息编辑
- 家庭成员管理
- 财务目标设置

### 8.2 我的家庭（所有用户）
- 查看家庭基本信息
- 编辑家庭基本信息（家庭名称、年度支出、紧急储备、财务目标）
- 查看家庭成员列表
- **不可修改**：成员角色、成员状态

## 9. 错误处理

### 9.1 常见错误码
- `401 Unauthorized` - Token无效或过期
- `403 Forbidden` - 无权限访问
- `404 Not Found` - 资源不存在

### 9.2 前端处理策略
- 401错误：自动登出并跳转登录页
- 403错误：显示权限不足提示
- 网络错误：显示重试提示

## 10. 未来扩展

### 10.1 潜在增强
- 细粒度权限（查看、编辑、删除）
- 审计日志（记录关键操作）
- 会话管理（多设备登录控制）
- 密码强度要求（大小写、数字、特殊字符）
- 双因素认证（2FA）

### 10.2 多家庭支持
当前系统架构支持多家庭，但用户界面尚未实现：
- 用户可属于多个家庭
- 切换家庭上下文
- 跨家庭数据共享策略

## 11. 测试建议

### 11.1 功能测试
- 不同角色登录验证
- 权限边界测试（跨家庭访问）
- Token过期处理
- 并发会话测试

### 11.2 安全测试
- SQL注入防护
- XSS攻击防护
- CSRF防护
- 密码暴力破解防护

---

**文档版本**: 1.0
**最后更新**: 2025-01-09
**维护者**: Development Team

# Environment Profiles Configuration

## 概述

项目使用 Spring Boot Profiles 分离开发环境和生产环境配置。

## 环境对照表

| 环境 | Profile | 启动方式 | SQL日志 | 应用日志级别 | DevTools |
|------|---------|---------|---------|-------------|----------|
| **开发环境** | `dev` | `./backend/start.sh` | ✅ 显示 | DEBUG | ✅ 启用 |
| **生产环境** | `prod` | `docker-compose up` | ❌ 关闭 | INFO | ❌ 禁用 |

## 配置文件说明

### 1. `application.properties` (共享配置)
包含所有环境通用的配置：
- 数据库连接配置
- JWT 配置
- 服务器端口配置
- Claude API 配置
- Profile 默认值: `dev`

### 2. `application-dev.properties` (开发环境)
```properties
# SQL 显示和格式化
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# 详细日志
logging.level.com.finance=debug
logging.level.org.springframework.web=debug
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=trace  # SQL参数绑定
```

**适用场景**:
- 本地开发调试
- 追踪 SQL 查询
- 查看详细的业务逻辑日志

### 3. `application-prod.properties` (生产环境)
```properties
# 不显示 SQL
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

# 精简日志
logging.level.com.finance=info
logging.level.org.springframework.web=info

# 禁用 DevTools
spring.devtools.restart.enabled=false
spring.devtools.livereload.enabled=false
```

**适用场景**:
- Docker 生产部署
- 减少日志输出
- 优化性能

## 使用方法

### 开发环境 (本地)

```bash
# 方法1: 使用 start.sh (推荐)
./backend/start.sh
# 自动设置 SPRING_PROFILES_ACTIVE=dev

# 方法2: 手动指定 profile
cd backend
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

**启动日志**:
```
✅ Environment variables loaded
   DB: finance_user@localhost:3306/finance
   JWT_SECRET: your-256-bit-secret-key-here...
🔧 Using profile: dev (development environment)
🚀 Starting Spring Boot application...

...
2025-01-13 09:05:12.345  INFO 12345 --- [           main] c.f.a.PersonalFinanceApplication        : The following 1 profile is active: "dev"
...
Hibernate: select * from users where username=?
```

### 生产环境 (Docker)

```bash
# 启动 Docker Compose
docker-compose up -d

# 查看日志
docker-compose logs -f backend
```

**启动日志**:
```
2025-01-13 09:10:15.123  INFO 1 --- [           main] c.f.a.PersonalFinanceApplication        : The following 1 profile is active: "prod"
...
# 不会显示 SQL 语句
# 只显示 INFO 及以上级别日志
```

**Profile 来源**:
1. `Dockerfile`: `ENV SPRING_PROFILES_ACTIVE=prod`
2. `docker-compose.yml`: `SPRING_PROFILES_ACTIVE: prod`

## 临时切换 Profile

### 本地临时使用生产配置
```bash
cd backend
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

### Docker 临时使用开发配置
```bash
# 修改 docker-compose.yml
environment:
  SPRING_PROFILES_ACTIVE: dev

# 或者通过环境变量
SPRING_PROFILES_ACTIVE=dev docker-compose up
```

## 验证当前 Profile

### 方法1: 查看启动日志
```
The following 1 profile is active: "dev"   # 开发环境
The following 1 profile is active: "prod"  # 生产环境
```

### 方法2: 访问 Actuator
```bash
curl http://localhost:8080/api/actuator/env | jq '.propertySources[] | select(.name | contains("applicationConfig")) | .properties."spring.profiles.active"'
```

### 方法3: 观察日志输出
- **看到 SQL 语句** → dev profile
- **看不到 SQL 语句** → prod profile

## 添加新的 Profile (可选)

如需添加测试环境、预发布环境等:

```bash
# 创建新 profile 配置
touch backend/src/main/resources/application-staging.properties

# 使用新 profile
SPRING_PROFILES_ACTIVE=staging ./backend/start.sh
```

## 常见问题

### Q: 本地开发看不到 SQL 日志?
**A**: 检查是否正确使用了 dev profile:
```bash
# 确保使用 start.sh
./backend/start.sh

# 或手动指定
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

### Q: Docker 生产环境日志太多?
**A**: 检查 `docker-compose.yml` 是否设置了 `SPRING_PROFILES_ACTIVE: prod`

### Q: 如何在特定代码中判断当前环境?
**A**: 使用 `@Profile` 注解或注入 `Environment`:
```java
@Autowired
private Environment env;

public void someMethod() {
    if (Arrays.asList(env.getActiveProfiles()).contains("dev")) {
        // 开发环境逻辑
    }
}
```

## 配置文件优先级

1. 环境变量: `SPRING_PROFILES_ACTIVE`
2. `application-{profile}.properties` (覆盖)
3. `application.properties` (基础)

**示例**: 如果 `application.properties` 和 `application-dev.properties` 都定义了 `logging.level.com.finance`:
- dev profile 激活时 → 使用 `application-dev.properties` 的值
- prod profile 激活时 → 使用 `application-prod.properties` 的值

## 总结

✅ **开发环境**: 使用 `./backend/start.sh` → 自动 dev profile
✅ **生产环境**: 使用 `docker-compose up` → 自动 prod profile
✅ **无需手动切换**: 自动根据启动方式选择正确的 profile

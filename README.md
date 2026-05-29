# Junction

Spring Boot 后端项目模板（提取自 saoma 项目）。

## 技术栈

- Java 17 + Spring Boot 4.0.2
- MyBatis 4.0.1（XML mapper 模式）
- MySQL 8.x（HikariCP 连接池）
- Redis 7.x（Lettuce 客户端）
- SpringDoc OpenAPI 2.8.4（Swagger UI）
- 阿里云 OSS + STS 临时凭证（前端直传）
- 微信小程序登录

## 已包含的模块

| 模块 | 路径 | 说明 |
|---|---|---|
| 统一返回 / 业务异常 / 全局异常 / ThreadLocal 用户上下文 | `common/` | 跨项目通用 |
| Web/Redis/Async/OpenAPI/OSS/RateLimit/InternalApi/WeChat 配置 | `config/` | 全部用 `@ConfigurationProperties` |
| 用户模块 + 微信登录 + Bearer Token 鉴权 | `controller/AuthController`, `controller/UserController`, `service/AuthService`, `service/WeChatService`, `interceptor/AuthInterceptor` | Redis 存 token，7 天过期 |
| 阿里云 OSS 上传 | `controller/OssController`, `controller/UploadController`, `service/OssService` | 服务端代传 + 前端直传（STS + V4 PostPolicy） |
| 限流 + 风控 | `interceptor/RateLimitInterceptor`, `service/RateLimitService`, `service/RiskControlService` | Redis 时间窗口分桶；自动作弊标记 |
| 内部接口 API Key 鉴权 | `interceptor/ApiKeyInterceptor` | `X-API-Key` header，作用于 `/api/internal/**` |
| 定时任务示例 | `scheduler/SampleScheduler` | `@Scheduled` + Redis 幂等锁 |
| Swagger UI | `config/OpenApiConfig` + 控制器注解 | 访问 `/swagger-ui.html` |

## 启动

```bash
# 1. 准备 MySQL 与 Redis
mysql -uroot -p < src/main/resources/schema.sql

# 2. 配置环境变量（或修改 application.yml）
export WECHAT_APP_ID=...
export WECHAT_APP_SECRET=...
export OSS_ACCESS_KEY_ID=...
export OSS_ACCESS_KEY_SECRET=...
export OSS_BUCKET=...
export OSS_ROLE_ARN=...

# 3. 启动
./mvnw spring-boot:run

# 4. 访问 Swagger
open http://localhost:8080/swagger-ui.html
```

## Profile（环境配置）

同一份 jar 通过 `--spring.profiles.active=xxx` 切换环境。Spring Boot 会先加载 `application.yml` 作为基线，再用 `application-{profile}.yml` 覆盖。

| Profile | 配置文件 | 端口 | 日志 | 用途 |
|---|---|---|---|---|
| `default` / 不指定 | `application.yml` | 8080 | 控制台 + 文件，DEBUG，mapper TRACE | 本地调试 |
| `test` | `application.yml` + `application-test.yml` | 8080 | 控制台 + 文件，DEBUG，mapper TRACE | 服务器测试环境 |
| `prod` | `application.yml` + `application-prod.yml` | 8081 | 仅文件，INFO/WARN，HikariCP 调优 | 生产环境 |

> `application-test.yml` 默认使用 `junction_test` 数据库；`application-prod.yml` 默认使用 `junction_prod` 数据库。敏感信息(账号/密码/AppSecret)统一通过环境变量注入，见下方部署章节。

日志路径：`JUNCTION_LOG_PATH` 环境变量覆盖，默认 `/var/log/junction`。

## 拦截器顺序

```
请求 → AuthInterceptor (order=1) → RateLimitInterceptor (order=2) → Controller
                                  ApiKeyInterceptor (order=3, /api/internal/** 专用)
```

## 构建

```bash
# 一键脚本（跳过测试）
./build.sh

# 或手动
./mvnw clean package -DskipTests

# 跑测试再打包
./mvnw clean package
```

构建产物：`target/junction-0.0.1-SNAPSHOT.jar`（Spring Boot fat jar，可直接 `java -jar` 启动）。

本地直接运行：

```bash
# 默认 profile（控制台 DEBUG 日志）
java -jar target/junction-0.0.1-SNAPSHOT.jar

# 指定测试环境 profile（加载 application-test.yml）
java -jar target/junction-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

# 指定生产环境 profile（加载 application-prod.yml）
java -jar target/junction-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 注入环境变量
DB_USER=xxx DB_PASS=xxx WECHAT_APP_ID=xxx \
  java -jar target/junction-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 部署（推荐：Linux + systemd，test + prod 双实例）

服务器上**同一个 jar 启两份实例**：test 与 prod 各占一个 systemd 服务、一个端口、一个数据库。dev 保持本地默认配置，服务器测试环境使用 `--spring.profiles.active=test`，生产环境使用 `--spring.profiles.active=prod`。

```
            同一台/不同服务器
┌──────────────────────────────────────────────┐
│  /opt/junction/junction.jar  (一份 jar)       │
│                                              │
│  ┌──────────────────┐  ┌──────────────────┐  │
│  │ junction-test    │  │ junction-prod    │  │
│  │ profile=test     │  │ profile=prod     │  │
│  │ port 8080        │  │ port 8081        │  │
│  │ DB junction_test │  │ DB junction_prod │  │
│  │ app-test.yml     │  │ app-prod.yml     │  │
│  └──────────────────┘  └──────────────────┘  │
└──────────────────────────────────────────────┘
```

### 1. 服务器准备

```bash
sudo apt update && sudo apt install -y openjdk-17-jdk

sudo useradd -r -s /bin/false junction
sudo mkdir -p /opt/junction /var/log/junction/test /var/log/junction/prod
sudo chown -R junction:junction /opt/junction /var/log/junction
```

### 2. 上传 jar 包

```bash
./build.sh
scp target/junction-0.0.1-SNAPSHOT.jar user@server:/tmp/
ssh user@server "sudo mv /tmp/junction-0.0.1-SNAPSHOT.jar /opt/junction/junction.jar \
                 && sudo chown junction:junction /opt/junction/junction.jar"
```

> 升级时只需替换 jar，再 `sudo systemctl restart junction-test`（或 prod）。

### 3. 两份 systemd 单元文件

仓库根目录已提供 `junction-test.service` 和 `junction-prod.service`，可按需复制到 `/etc/systemd/system/` 后再调整环境变量。

`/etc/systemd/system/junction-test.service`：

```ini
[Unit]
Description=Junction (TEST)
After=network.target mysql.service redis.service

[Service]
Type=simple
User=junction
Group=junction
WorkingDirectory=/opt/junction

Environment=JUNCTION_LOG_PATH=/var/log/junction/test
Environment=DB_HOST=localhost
Environment=DB_PORT=3306
Environment=DB_NAME=junction_test
Environment=DB_USER=junction_test
Environment=DB_PASS=changeme-test
Environment=REDIS_HOST=localhost
Environment=REDIS_PORT=6379
Environment=REDIS_PASSWORD=
Environment=REDIS_DB=0
Environment=INTERNAL_API_KEY=dev-internal-key
Environment=WECHAT_APP_ID=
Environment=WECHAT_APP_SECRET=
Environment=OSS_ENABLED=false
Environment=OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
Environment=OSS_ACCESS_KEY_ID=
Environment=OSS_ACCESS_KEY_SECRET=
Environment=OSS_BUCKET=junction-test
Environment=OSS_REGION=cn-beijing
Environment=OSS_ROLE_ARN=

ExecStartPre=+/bin/mkdir -p /var/log/junction/test
ExecStartPre=+/bin/chown junction:junction /var/log/junction/test

ExecStart=/usr/bin/java \
  -Xms256m -Xmx512m \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/junction/test/heap-dump.hprof \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -jar /opt/junction/junction.jar \
  --spring.profiles.active=test

Restart=on-failure
RestartSec=10
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
SyslogIdentifier=junction-test
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

`/etc/systemd/system/junction-prod.service`：

```ini
[Unit]
Description=Junction (PROD)
After=network.target mysql.service redis.service

[Service]
Type=simple
User=junction
Group=junction
WorkingDirectory=/opt/junction

Environment=JUNCTION_LOG_PATH=/var/log/junction/prod
Environment=DB_HOST=localhost
Environment=DB_PORT=3306
Environment=DB_NAME=junction_prod
Environment=DB_USER=junction_prod
Environment=DB_PASS=changeme-prod
Environment=REDIS_HOST=localhost
Environment=REDIS_PORT=6379
Environment=REDIS_PASSWORD=
Environment=REDIS_DB=1
Environment=INTERNAL_API_KEY=prod-internal-key
Environment=WECHAT_APP_ID=
Environment=WECHAT_APP_SECRET=
Environment=OSS_ENABLED=true
Environment=OSS_ENDPOINT=oss-cn-beijing.aliyuncs.com
Environment=OSS_ACCESS_KEY_ID=
Environment=OSS_ACCESS_KEY_SECRET=
Environment=OSS_BUCKET=junction-prod
Environment=OSS_REGION=cn-beijing
Environment=OSS_ROLE_ARN=

ExecStartPre=+/bin/mkdir -p /var/log/junction/prod
ExecStartPre=+/bin/chown junction:junction /var/log/junction/prod

ExecStart=/usr/bin/java \
  -Xms512m -Xmx1g \
  -XX:+UseG1GC \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/junction/prod/heap-dump.hprof \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai \
  -jar /opt/junction/junction.jar \
  --spring.profiles.active=prod

Restart=on-failure
RestartSec=10
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal
SyslogIdentifier=junction-prod
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
```

> `application-test.yml` 里的多数配置都有默认值；上面的 `Environment=` 是服务器部署模板。`application-prod.yml` 里的数据库账号、微信、OSS 等配置部署前按实际值替换。

### 4. 启停命令（每个服务独立）

```bash
sudo systemctl daemon-reload

# 启用并启动两套
sudo systemctl enable --now junction-test
sudo systemctl enable --now junction-prod

# 重启某一套
sudo systemctl restart junction-test
sudo systemctl restart junction-prod

# 状态
sudo systemctl status junction-test
sudo systemctl status junction-prod

# 实时日志（journald）
sudo journalctl -u junction-test -f
sudo journalctl -u junction-prod -f

# logback 落地的应用日志（按 profile 分目录）
tail -f /var/log/junction/test/app.log
tail -f /var/log/junction/test/error.log
tail -f /var/log/junction/prod/app.log
tail -f /var/log/junction/prod/error.log

# 停止
sudo systemctl stop junction-test
sudo systemctl stop junction-prod
```

### 5. 数据库与 Redis 隔离

```bash
# 两个数据库
mysql -u root -p -e "CREATE DATABASE junction_test DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p -e "CREATE DATABASE junction_prod DEFAULT CHARACTER SET utf8mb4;"

# 各自初始化
mysql -u root -p junction_test < src/main/resources/schema.sql
mysql -u root -p junction_prod < src/main/resources/schema.sql

# Redis 用同一个实例的不同 db（test=0, prod=1，已写在 systemd Environment 中）；
# 如有条件,生产建议用独立 Redis 实例
```

### 6. 排错速查

```bash
# 启动失败
sudo journalctl -u junction-prod -n 100 --no-pager

# 日志目录权限问题
sudo journalctl -u junction-prod -n 100 | grep -E "Failed to create parent directories|openFile"

# 端口占用 / 是否两套都在跑
sudo lsof -i :8080
sudo lsof -i :8081
sudo systemctl list-units 'junction*' --no-pager

# 健康检查（注意端口；dev 本地默认也是 8080）
curl -i http://localhost:8080/swagger-ui.html   # test
curl -i http://localhost:8081/swagger-ui.html   # prod

# 验证当前进程加载了哪个 profile
ps -ef | grep junction.jar | grep -E 'spring.profiles.active'
```

## 部署（可选：Docker）

模板默认未提供 Dockerfile。若要容器化，参考下面的最小示例（test/prod 用同一镜像，差别在启动参数和环境变量）：

```dockerfile
# Dockerfile
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/junction-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080 8081
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
```

```bash
./build.sh
docker build -t junction:latest .

# test
docker run -d --name junction-test \
  -p 8080:8080 \
  -e JUNCTION_LOG_PATH=/var/log/junction/test \
  -e DB_NAME=junction_test \
  -v /var/log/junction/test:/var/log/junction/test \
  junction:latest --spring.profiles.active=test

# prod
docker run -d --name junction-prod \
  -p 8081:8081 \
  -e JUNCTION_LOG_PATH=/var/log/junction/prod \
  -e DB_USER=junction_prod \
  -e DB_PASS=changeme-prod \
  -e WECHAT_APP_ID=xxx \
  -e WECHAT_APP_SECRET=xxx \
  -v /var/log/junction/prod:/var/log/junction/prod \
  junction:latest --spring.profiles.active=prod
```

## 后续扩展

- 新增业务表：`entity/` + `mapper/` + `mapper/xml`
- 新增业务接口：`controller/` + `service/` + `service/impl/`，加 `@Tag` `@Operation` 即出 Swagger 文档
- 新增定时任务：复制 `SampleScheduler` 改 cron 与业务即可
- 收紧某个接口的限流：在 `RateLimitInterceptor.preHandle` 按 uri 加判断，复用 `rateLimitService.isRateLimited(...)`

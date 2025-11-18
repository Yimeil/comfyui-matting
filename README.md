# ComfyUI Matting Service

**基于 Vue 3 + Spring Boot + ComfyUI 的智能抠图服务**

## 🎯 项目概述

这是一个专业的 ComfyUI 抠图服务，提供简洁高效的图像抠图解决方案，支持普通抠图和关键字抠图两种模式。

### 核心特性

- 🎨 **现代化前端** - Vue 3 响应式用户界面
- ☕ **Spring Boot + JDK 21** - 高性能后端服务
- 🤖 **SAM 智能抠图** - 基于 Segment Anything Model
- 🔍 **关键字抠图** - 结合 GroundingDINO 的语义抠图
- 📦 **开箱即用** - Maven 一键构建运行

## 🏗️ 架构设计

### 整体架构流程

```
┌─────────────────┐
│   Vue 3 前端    │ (用户界面)
│ index.html      │
│ matting-        │
│ keyword.html    │
└────────┬────────┘
         │ HTTP POST /api/matting/*
         ▼
┌─────────────────┐
│MattingController│ (Spring Boot 控制器)
│  处理 API 请求  │
└────────┬────────┘
         │ 调用业务方法
         ▼
┌─────────────────┐
│ ComfyUIService  │ (ComfyUI API 封装)
│ - 上传图片      │
│ - 加载工作流    │
│ - 执行任务      │
│ - 下载结果      │
└────────┬────────┘
         │ HTTP API 调用
         ▼
┌─────────────────┐
│   ComfyUI API   │ (外部服务)
│  执行工作流     │
└─────────────────┘
```

## 📁 项目结构

```
comfyui-matting/
├── src/
│   ├── main/
│   │   ├── java/com/yimeil/comfyui/
│   │   │   ├── ComfyuiMattingApplication.java  # 启动类
│   │   │   │
│   │   │   ├── controller/                     # 控制器层
│   │   │   │   ├── MattingController.java      # 抠图 API
│   │   │   │   └── PageController.java         # 页面路由
│   │   │   │
│   │   │   ├── service/                        # 服务层
│   │   │   │   └── ComfyUIService.java         # ComfyUI 核心服务
│   │   │   │
│   │   │   ├── model/                          # 数据模型
│   │   │   │   ├── MattingRequest.java
│   │   │   │   ├── MattingResult.java
│   │   │   │   └── ApiResponse.java
│   │   │   │
│   │   │   └── config/                         # 配置类
│   │   │       ├── ComfyUIConfig.java
│   │   │       └── WebConfig.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml                 # 应用配置
│   │       ├── static/                         # 静态资源
│   │       │   ├── index.html                  # 普通抠图页面
│   │       │   └── matting-keyword.html        # 关键字抠图页面
│   │       └── workflows/                      # 工作流目录
│   │           ├── sam_matting.json            # SAM 抠图工作流
│   │           ├── matting_keyword_api.json    # 关键字抠图工作流
│   │           ├── batch_matting_api.json      # 批量抠图工作流
│   │           └── ...                         # 其他工作流
│   │
│   └── test/                                    # 测试
│
├── pom.xml                                      # Maven 配置 (JDK 21)
├── README.md                                    # 本文档
└── output/                                      # 输出目录
```

## 🚀 快速开始

### 前置要求

1. **JDK 21**
   ```bash
   java -version  # 应显示 "21.x.x"
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **ComfyUI 已运行** (默认端口 8188)
   ```bash
   # 启动 ComfyUI
   python main.py
   ```

### 配置 ComfyUI 地址

编辑 `src/main/resources/application.yml`:

```yaml
comfyui:
  api:
    base-url: http://127.0.0.1:8188  # 修改为你的 ComfyUI 地址
```

### 运行应用

```bash
# 方式 1: Maven 运行
mvn spring-boot:run

# 方式 2: 打包运行
mvn clean package -DskipTests
java -jar target/comfyui-matting-2.0.0.jar
```

### 访问应用

- **普通抠图**: http://localhost:8080
- **关键字抠图**: http://localhost:8080/matting-keyword

## 🎨 功能说明

### 1. 普通抠图 (SAM)

使用 Segment Anything Model 进行智能抠图，无需关键字。

**使用步骤:**
1. 访问 http://localhost:8080
2. 上传图片
3. 调整 SAM 参数（可选）
4. 点击"开始执行"
5. 下载抠图结果

### 2. 关键字抠图 (SAM + GroundingDINO)

基于语义关键字进行精准抠图。

**使用步骤:**
1. 访问 http://localhost:8080/matting-keyword
2. 上传图片
3. 输入关键字（如"红色袜子"、"人脸"、"汽车"）
4. 调整参数（可选）
5. 点击"开始抠图"
6. 下载抠图结果

**支持的关键字示例:**
- 中文: "红色袜子"、"人脸"、"猫咪"、"汽车"
- 英文: "red socks"、"face"、"cat"、"car"

## 🔍 API 端点说明

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/matting/execute` | POST | 执行普通抠图 |
| `/api/matting/keyword` | POST | 执行关键字抠图 |
| `/api/matting/status` | GET | 检查服务器状态 |

### API 使用示例

#### 普通抠图

```bash
curl -X POST http://localhost:8080/api/matting/execute \
  -F "image=@test.jpg" \
  -F "threshold=0.3" \
  -F "alphaMatting=true"
```

#### 关键字抠图

```bash
curl -X POST http://localhost:8080/api/matting/keyword \
  -F "image=@test.jpg" \
  -F "keyword=红色袜子" \
  -F "translateFrom=chinese" \
  -F "threshold=0.3"
```

**响应示例:**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "outputFilename": "img_00005_.png",
    "outputUrl": "/output/img_00005_.png",
    "promptId": "abc-123-def",
    "executionTime": 5230
  }
}
```

## ⚙️ 配置参数

### application.yml 完整配置

```yaml
server:
  port: 8080                          # 应用端口

comfyui:
  api:
    base-url: http://127.0.0.1:8188  # ComfyUI 地址
    connect-timeout: 10000            # 连接超时（毫秒）
    read-timeout: 300000              # 读取超时（毫秒）

  workflow:
    directory: workflows              # 工作流目录
    default-workflow: sam_matting.json # 默认工作流

output:
  directory: output                   # 输出目录
  auto-create: true                   # 自动创建输出目录

spring:
  servlet:
    multipart:
      max-file-size: 50MB             # 最大文件大小
      max-request-size: 50MB          # 最大请求大小
```

## 📊 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | Vue 3 | 3.x (CDN) |
| **前端库** | Axios | Latest |
| **后端框架** | Spring Boot | 3.2.0 |
| **Java** | OpenJDK | 21 |
| **构建工具** | Maven | 3.9+ |
| **HTTP 客户端** | Apache HttpClient | 5.3 |
| **JSON 处理** | Jackson | (Spring Boot 内置) |
| **日志** | Slf4j + Logback | (Spring Boot 内置) |

## 🐛 故障排查

### 无法连接 ComfyUI

```bash
# 检查 ComfyUI 是否运行
curl http://127.0.0.1:8188/system_stats

# 检查配置
cat src/main/resources/application.yml | grep base-url
```

### JDK 版本不匹配

```bash
# 检查 Java 版本
java -version

# 应显示 21.x.x，如果不是，请安装 JDK 21
```

### Maven 编译错误

```bash
# 清理并重新编译
mvn clean install -DskipTests -U
```

### 关键字抠图中文乱码

确保后端正确设置 UTF-8 编码。已在 `ComfyUIService.java` 中使用 `ContentType.APPLICATION_JSON` 解决。

### 前端显示"抠图失败: undefined"

确保前端正确解析 `ApiResponse` 格式：
```javascript
if (result.code === 200 && result.data && result.data.success) {
    // 使用 result.data.outputUrl
}
```

## 📚 开发指南

### 日志级别

编辑 `application.yml`:

```yaml
logging:
  level:
    com.yimeil.comfyui: DEBUG         # 应用日志
    org.springframework: INFO          # Spring 框架日志
```

### 启用 CORS (跨域支持)

如果需要从其他域访问 API，编辑 `WebConfig.java`:

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE");
}
```

## 🚢 生产部署

### 打包

```bash
mvn clean package -DskipTests
```

### 运行

```bash
java -jar target/comfyui-matting-2.0.0.jar
```

### 后台运行

```bash
nohup java -jar target/comfyui-matting-2.0.0.jar > app.log 2>&1 &
```

### Docker 部署

创建 `Dockerfile`:

```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY target/comfyui-matting-2.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建并运行:

```bash
docker build -t comfyui-matting:2.0 .
docker run -p 8080:8080 comfyui-matting:2.0
```

## 📄 许可

MIT License

## 🔗 相关链接

- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) - 强大的 Stable Diffusion GUI
- [Vue 3 文档](https://vuejs.org/) - Vue.js 官方文档
- [Spring Boot 文档](https://spring.io/projects/spring-boot) - Spring Boot 官方文档
- [JDK 21 特性](https://openjdk.org/projects/jdk/21/) - Java 21 新特性

---

**简洁、高效、专业的抠图服务！** 🚀

# ComfyUI Matting Service V2.0

**基于 Vue 3 + Spring Boot + Claude Skills + ComfyUI 的智能抠图服务**

## 🎯 项目概述

这是一个全新架构的 ComfyUI 抠图服务，通过 Claude Skills 实现前端与 ComfyUI 的解耦，支持灵活扩展不同的图像处理功能。

### 核心特性

- 🎨 **Vue 3 前端** - 现代化的响应式用户界面
- ☕ **Spring Boot + JDK 21** - 高性能后端服务
- 🤖 **Claude Skills 架构** - 基于技能的模块化设计
- 🔄 **灵活扩展** - 轻松添加新的图像处理技能
- 📦 **开箱即用** - Maven 一键构建运行

## 🏗️ 架构设计

### 整体架构流程

```
┌─────────────────┐
│   Vue 3 前端    │ (用户界面)
│  index.html     │
└────────┬────────┘
         │ HTTP POST /api/skill/matting
         ▼
┌─────────────────┐
│ SkillController │ (Spring Boot 控制器)
│  处理 API 请求  │
└────────┬────────┘
         │ 调用 executeMattingSkill()
         ▼
┌─────────────────┐
│ SkillExecutor   │ (技能执行器)
│ 读取 Skill 定义 │
│ 验证参数        │
└────────┬────────┘
         │ 引用 .claude/skills/matting.md
         │ 调用 ComfyUIService
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
├── .claude/                          # Claude Skills 定义目录
│   └── skills/
│       └── matting.md                # 抠图技能定义
│
├── src/
│   ├── main/
│   │   ├── java/com/yimeil/comfyui/
│   │   │   ├── ComfyuiMattingApplication.java  # 启动类
│   │   │   │
│   │   │   ├── controller/                     # 控制器层
│   │   │   │   ├── SkillController.java        # Claude Skills API
│   │   │   │   ├── MattingController.java      # 传统抠图 API (保留)
│   │   │   │   └── PageController.java         # 页面路由
│   │   │   │
│   │   │   ├── service/                        # 服务层
│   │   │   │   ├── SkillExecutor.java          # 技能执行器 ⭐ 新增
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
│   │       │   └── index.html                  # Vue 3 前端 ⭐ 新增
│   │       └── workflows/                      # 工作流目录
│   │           └── sam_matting.json
│   │
│   └── test/                                    # 测试
│
├── pom.xml                                      # Maven 配置 (JDK 21)
├── README_V2.md                                 # 本文档
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

打开浏览器访问：**http://localhost:8080**

## 🎨 使用说明

### Web 界面使用

1. **选择功能** - 点击"智能抠图"按钮
2. **上传图片** - 拖拽或点击上传图片文件
3. **调整参数** - 根据需要调整 SAM 阈值和边缘优化参数
4. **执行处理** - 点击"开始执行"按钮
5. **下载结果** - 处理完成后下载抠图结果

### API 使用

#### 执行抠图 Skill

```bash
curl -X POST http://localhost:8080/api/skill/matting \
  -F "image=@test.jpg" \
  -F "threshold=0.3" \
  -F "alphaMatting=true"
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "outputFilename": "matting_result_12345.png",
    "outputUrl": "/output/matting_result_12345.png",
    "promptId": "abc-123-def",
    "executionTime": 5230
  }
}
```

#### 获取可用 Skills

```bash
curl http://localhost:8080/api/skill/list
```

#### 获取 Skill 信息

```bash
curl http://localhost:8080/api/skill/matting/info
```

## 🔧 添加新的 Skill

### 步骤 1: 创建 Skill 定义

在 `.claude/skills/` 目录下创建新的 Skill 定义文件，例如 `enhance.md`:

```markdown
# Image Enhancement Skill

This skill provides image enhancement capabilities.

## Input Parameters
- imagePath: Path to input image
- brightness: Brightness adjustment (-100 to 100)
- contrast: Contrast adjustment (-100 to 100)

## Output
- success: Operation status
- outputFilename: Enhanced image filename
- outputUrl: URL to download result
```

### 步骤 2: 在 SkillExecutor 中实现

```java
public MattingResult executeEnhanceSkill(MultipartFile imageFile, EnhanceRequest request) {
    log.info("【Enhance Skill】开始执行");

    // 验证 Skill 定义
    validateSkillExists("enhance");

    // 调用 ComfyUIService 执行增强任务
    return comfyUIService.runEnhancement(imageFile, request);
}
```

### 步骤 3: 添加 Controller 端点

```java
@PostMapping("/enhance")
public ApiResponse<MattingResult> executeEnhanceSkill(
        @RequestParam("image") MultipartFile imageFile,
        @RequestParam(value = "brightness", required = false) Integer brightness,
        @RequestParam(value = "contrast", required = false) Integer contrast) {
    // 执行 Enhance Skill
    return skillExecutor.executeEnhanceSkill(imageFile, request);
}
```

### 步骤 4: 更新前端 UI

在 Vue 前端添加新的 Skill 按钮和参数控制。

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

## 🔍 API 端点说明

### Claude Skills API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/skill/matting` | POST | 执行抠图 Skill |
| `/api/skill/list` | GET | 获取所有可用 Skills |
| `/api/skill/{skillName}/info` | GET | 获取特定 Skill 信息 |

### 传统 API (向后兼容)

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/matting/execute` | POST | 直接执行抠图 |
| `/api/matting/status` | GET | 检查服务器状态 |

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
    default: sam_matting.json         # 默认工作流

output:
  directory: output                   # 输出目录
  auto-create: true                   # 自动创建输出目录

spring:
  servlet:
    multipart:
      max-file-size: 50MB             # 最大文件大小
      max-request-size: 50MB          # 最大请求大小
```

## 🌟 版本对比

| 特性 | V1.0 (Thymeleaf) | V2.0 (Vue + Skills) |
|------|------------------|---------------------|
| **JDK 版本** | 17 | 21 ⭐ |
| **前端技术** | Thymeleaf | Vue 3 ⭐ |
| **架构模式** | MVC | Skills-based ⭐ |
| **扩展性** | 中等 | 优秀 ⭐ |
| **模块化** | 低 | 高 ⭐ |
| **用户体验** | 良好 | 优秀 ⭐ |
| **API 设计** | RESTful | RESTful + Skills ⭐ |

## 🔒 Claude Skills 架构优势

### 1. 解耦与模块化
- 前端只需关注 Skill 名称，无需了解底层实现
- 每个 Skill 独立定义，易于维护和测试

### 2. 易于扩展
- 添加新功能只需创建新 Skill 定义
- 无需修改核心业务逻辑

### 3. 统一管理
- 所有 Skills 定义集中在 `.claude/skills/` 目录
- 便于版本控制和文档管理

### 4. 灵活组合
- 未来可以实现 Skill 链式调用
- 支持复杂的图像处理流程

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

### Skill 执行失败

检查日志中的详细错误信息，常见原因：
- Skill 定义文件不存在
- 参数验证失败
- ComfyUI 服务不可用
- 工作流文件缺失或格式错误

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

**V2.0 - 更智能、更模块化、更易扩展！** 🚀

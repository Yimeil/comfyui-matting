# ComfyUI Matting Service - Spring Boot 版本

**简单易用的 ComfyUI 抠图服务** - 参考 [word2picture](https://github.com/treeHeartPig/word2picture) 的 Spring Boot 架构

## 📋 项目概述

这是一个基于 Spring Boot 的完整 Web 应用，通过简洁的界面调用 ComfyUI API 进行智能图像抠图。

### 核心特性

- 🌐 **完整 Web 应用** - Spring Boot + Thymeleaf 前后端完整解决方案
- 🎨 **可视化界面** - 美观易用的 Web 操作界面
- ⚙️ **简单配置** - 只需在 application.yml 配置 ComfyUI 地址
- 📦 **开箱即用** - Maven 一键构建运行
- 🔄 **灵活扩展** - 支持添加任意 ComfyUI 工作流

## 🏗️ 项目结构

```
comfyui-matting/
├── src/
│   ├── main/
│   │   ├── java/com/yimeil/comfyui/
│   │   │   ├── ComfyuiMattingApplication.java  # 启动类
│   │   │   ├── controller/                     # 控制器
│   │   │   │   ├── MattingController.java      # 抠图 API
│   │   │   │   └── PageController.java         # 页面路由
│   │   │   ├── service/                        # 服务层
│   │   │   │   └── ComfyUIService.java         # ComfyUI 核心服务
│   │   │   ├── model/                          # 数据模型
│   │   │   │   ├── MattingRequest.java
│   │   │   │   ├── MattingResult.java
│   │   │   │   └── ApiResponse.java
│   │   │   └── config/                         # 配置类
│   │   │       ├── ComfyUIConfig.java
│   │   │       └── WebConfig.java
│   │   └── resources/
│   │       ├── application.yml                 # 应用配置
│   │       ├── workflows/                      # 工作流目录
│   │       │   └── sam_matting.json
│   │       └── templates/                      # 前端页面
│   │           └── index.html
│   └── test/                                    # 测试
├── pom.xml                                      # Maven 配置
├── output/                                      # 输出目录（自动创建）
└── README_JAVA.md
```

**对比 word2picture 项目：**
- ✅ 相同的架构思路：Spring Boot + 工作流 JSON
- ✅ 相同的配置方式：application.yml
- ✅ 相同的服务设计：ComfyUIService 统一封装
- ✨ 增强功能：添加了完整的 Web UI

## 🚀 快速开始

### 1. 前置要求

- **JDK 17+**
  ```bash
  java -version
  ```

- **Maven 3.6+**
  ```bash
  mvn -version
  ```

- **ComfyUI 已安装并运行** (默认端口 8188)
  ```bash
  # 启动 ComfyUI
  python main.py
  ```

### 2. 配置 ComfyUI 地址

编辑 `src/main/resources/application.yml`:

```yaml
comfyui:
  api:
    # ComfyUI 服务器地址（必须配置）
    base-url: http://127.0.0.1:8188
```

### 3. 构建并运行

```bash
# 方式 1: 使用 Maven 运行
mvn spring-boot:run

# 方式 2: 打包后运行
mvn clean package
java -jar target/comfyui-matting-1.0.0.jar
```

### 4. 访问应用

打开浏览器访问：**http://localhost:8080**

就这么简单！ 🎉

## 📖 使用指南

### Web 界面使用

1. **上传图片** - 点击或拖拽上传需要抠图的图片
2. **调整参数** - 根据需要调整抠图参数（可选）
   - SAM 阈值
   - Alpha Matting 选项
   - 边缘优化参数
3. **开始抠图** - 点击"开始抠图"按钮
4. **下载结果** - 处理完成后下载抠图结果

### API 使用

#### 执行抠图

```bash
curl -X POST http://localhost:8080/api/matting/execute \
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
    "outputFilename": "output_12345.png",
    "outputUrl": "/output/output_12345.png",
    "promptId": "abc-123-def",
    "executionTime": 5230
  }
}
```

#### 检查服务器状态

```bash
curl http://localhost:8080/api/matting/status
```

### Java 代码使用

```java
@Autowired
private ComfyUIService comfyUIService;

// 执行抠图
public void processImage(MultipartFile imageFile) {
    MattingRequest request = new MattingRequest();
    request.setThreshold(0.3);
    request.setAlphaMatting(true);

    MattingResult result = comfyUIService.runMatting(imageFile, request);

    if (result.isSuccess()) {
        System.out.println("输出文件: " + result.getOutputUrl());
    }
}
```

## 🔧 添加新工作流

### 1. 导出 ComfyUI 工作流

在 ComfyUI 中：
- 构建您的工作流
- 点击 "Save (API Format)"
- 保存到 `src/main/resources/workflows/your_workflow.json`

### 2. 使用新工作流

通过 API 指定工作流名称：

```bash
curl -X POST http://localhost:8080/api/matting/execute \
  -F "image=@test.jpg" \
  -F "workflowName=your_workflow.json"
```

或修改代码：

```java
request.setWorkflowName("your_workflow.json");
```

就这么简单！无需修改代码。

## ⚙️ 配置说明

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
```

### 修改端口

```yaml
server:
  port: 9090  # 修改为其他端口
```

### 修改 ComfyUI 地址

```yaml
comfyui:
  api:
    base-url: http://192.168.1.100:8188  # 远程 ComfyUI 服务器
```

## 📦 Maven 依赖

主要依赖：
- Spring Boot 3.2.0
- Spring Boot Web
- Spring Boot Thymeleaf
- Apache HttpClient 5
- Jackson
- Lombok

完整依赖请查看 `pom.xml`

## 🎨 内置工作流

### SAM 智能抠图 (sam_matting.json)

使用 Segment Anything Model 进行智能图像抠图。

**输入：** 图片文件

**输出：** 抠图后的 PNG 图片（带透明背景）

**可配置参数：**
- **threshold** (Double): SAM 检测阈值 (0.0-1.0)
- **alphaMatting** (Boolean): 是否启用边缘优化
- **alphaMattingForegroundThreshold** (Integer): 前景阈值 (200-255)
- **alphaMattingBackgroundThreshold** (Integer): 背景阈值 (0-50)
- **alphaMattingErodeSize** (Integer): 边缘腐蚀大小 (0-20)

## 🔍 故障排查

### 无法连接 ComfyUI

```bash
# 检查 ComfyUI 是否运行
curl http://127.0.0.1:8188/system_stats

# 检查配置文件中的地址是否正确
cat src/main/resources/application.yml
```

### Maven 构建失败

```bash
# 清理并重新构建
mvn clean install -U
```

### 端口被占用

修改 `application.yml` 中的端口号，或停止占用端口的程序。

### 文件上传失败

检查 `application.yml` 中的文件大小限制：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB        # 根据需要调整
      max-request-size: 50MB
```

## 🌟 为什么选择 Spring Boot 版本？

| 特性 | Python 版本 | Spring Boot 版本 |
|------|------------|-----------------|
| **技术栈** | Python/FastAPI | Java/Spring Boot |
| **Web UI** | ❌ 无 | ✅ 完整的 Web 界面 |
| **部署** | Python 环境 | JVM 环境 |
| **企业集成** | 中等 | ✅ 优秀（易集成企业系统） |
| **性能** | 快速 | 稳定 |
| **适用场景** | 轻量级服务 | ✅ 企业级应用 |

**参考项目：** [word2picture](https://github.com/treeHeartPig/word2picture) - 简洁的 Spring Boot + ComfyUI 架构

## 📸 界面预览

- 🎨 美观的渐变色界面
- 📤 拖拽上传支持
- ⚙️ 实时参数调整
- 🔄 实时进度显示
- 💾 一键下载结果

## 🚢 生产部署

### 打包

```bash
mvn clean package -DskipTests
```

### 运行

```bash
java -jar target/comfyui-matting-1.0.0.jar
```

### 后台运行

```bash
nohup java -jar target/comfyui-matting-1.0.0.jar > app.log 2>&1 &
```

### Docker 部署（可选）

```dockerfile
FROM openjdk:17-slim
COPY target/comfyui-matting-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📄 许可

MIT License

## 🔗 相关链接

- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) - 强大的 Stable Diffusion GUI
- [word2picture](https://github.com/treeHeartPig/word2picture) - 参考的 Spring Boot 架构
- [Segment Anything](https://github.com/facebookresearch/segment-anything) - Meta 的通用分割模型
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)

---

**完整的企业级解决方案！** 🚀

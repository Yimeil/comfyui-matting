# Java + HTML 图像抠图 Web 应用使用指南

## 📋 项目概述

这是一个轻量级的图像抠图 Web 应用，使用 **Java Spring Boot + HTML/JavaScript** 构建，替代了原来的 Gradio 方案，减少了 Python 依赖。

### 优势

✅ **轻量级** - 只需要 Java 运行环境，无需安装大量 Python 依赖
✅ **跨平台** - 支持 Windows、Linux、macOS
✅ **易部署** - 打包成单个 JAR 文件，一键启动
✅ **高性能** - Spring Boot 提供的高性能 Web 服务
✅ **现代化界面** - 响应式设计，支持移动端
✅ **无需额外前端框架** - 纯 HTML/JavaScript，零依赖

## 🚀 快速开始

### 前置要求

1. **Java 17 或更高版本**
   ```bash
   # 检查 Java 版本
   java -version
   ```

2. **Maven 3.6+ (可选，用于构建)**
   ```bash
   # 检查 Maven 版本
   mvn -version
   ```

3. **ComfyUI 服务器运行中**
   - 默认地址：`127.0.0.1:8188`
   - 确保已安装所需的自定义节点和 SAM 模型

### 安装步骤

#### 方式 A: 使用预构建的 JAR (推荐)

1. 下载或构建 JAR 文件
   ```bash
   # 构建项目
   mvn clean package
   ```

2. 运行应用
   ```bash
   java -jar target/matting-web-app-1.0.0.jar
   ```

3. 在浏览器中访问
   ```
   http://localhost:8080
   ```

#### 方式 B: 使用启动脚本

1. Linux/macOS
   ```bash
   chmod +x run_java_app.sh
   ./run_java_app.sh
   ```

2. Windows
   ```cmd
   run_java_app.bat
   ```

#### 方式 C: 开发模式 (使用 Maven)

```bash
# 直接运行（无需构建 JAR）
mvn spring-boot:run
```

## 📁 项目结构

```
comfyui-matting/
├── src/
│   └── main/
│       ├── java/com/comfyui/matting/
│       │   ├── MattingApplication.java      # 主应用类
│       │   ├── controller/
│       │   │   └── MattingController.java   # REST API 控制器
│       │   ├── service/
│       │   │   └── ComfyUIService.java      # ComfyUI 服务
│       │   ├── model/
│       │   │   └── MattingRequest.java      # 请求模型
│       │   └── config/
│       │       └── CorsConfig.java          # CORS 配置
│       └── resources/
│           ├── application.properties       # 应用配置
│           └── static/
│               └── index.html              # Web 界面
├── pom.xml                                  # Maven 配置
├── sam_mask_matting_api.json               # ComfyUI 工作流
└── JAVA_WEB_APP_GUIDE.md                   # 本文档
```

## ⚙️ 配置说明

### application.properties

```properties
# 服务器端口
server.port=8080

# ComfyUI 服务器地址
comfyui.server.host=127.0.0.1
comfyui.server.port=8188

# 文件上传限制
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

### 自定义配置

如果 ComfyUI 运行在不同的地址，可以通过以下方式配置：

1. **修改 application.properties**（推荐）

2. **环境变量**
   ```bash
   export COMFYUI_SERVER_HOST=192.168.1.100
   export COMFYUI_SERVER_PORT=8188
   java -jar matting-web-app-1.0.0.jar
   ```

3. **命令行参数**
   ```bash
   java -jar matting-web-app-1.0.0.jar \
     --comfyui.server.host=192.168.1.100 \
     --comfyui.server.port=8188
   ```

## 🎯 使用指南

### 1. 上传图像

- 点击左侧区域上传**原始图像**
- 点击右侧区域上传**蒙版图像**
- 支持拖拽上传
- 支持格式：PNG, JPG, JPEG

### 2. 选择预设或调整参数

#### 快速预设

- **默认** - 通用场景，平衡的参数设置
- **人像抠图** - 柔和边缘，适合人物照片
- **产品图** - 锐利边缘，适合电商产品
- **毛发细节** - 保留细节，适合毛发处理

#### 手动调整参数

| 参数 | 说明 | 调整建议 |
|-----|------|---------|
| **SAM 检测阈值** | 控制分割精度 | 提高值可获得更精确的分割 |
| **形态学核大小** | 填充孔洞 | 增大值可填充更大的孔洞 |
| **蒙版收缩** | 避免白边 | 负值越大，收缩越多 |
| **边缘模糊** | 柔和边缘 | 增大值可获得更柔和的边缘 |

### 3. 开始处理

点击 **"🚀 开始处理"** 按钮

- 应用会自动上传图像到 ComfyUI
- 提交工作流并开始处理
- 实时显示处理状态

### 4. 查看和下载结果

- 处理完成后自动显示结果
- 点击 **"📥 下载结果"** 保存图像
- 点击 **"🔄 重新开始"** 处理新图像

## 🔌 API 接口文档

### 健康检查

```http
GET /api/health
```

**响应示例：**
```json
{
  "status": "ok",
  "comfyui_connected": true
}
```

### 提交抠图任务

```http
POST /api/matting
Content-Type: multipart/form-data
```

**请求参数：**
| 参数 | 类型 | 必需 | 说明 |
|-----|------|------|------|
| image | File | 是 | 原始图像文件 |
| mask | File | 是 | 蒙版图像文件 |
| maskHintThreshold | Double | 否 | SAM 检测阈值 (默认: 0.6) |
| kernelSize | Integer | 否 | 形态学核大小 (默认: 6) |
| expand | Integer | 否 | 蒙版收缩量 (默认: -3) |
| blurRadius | Double | 否 | 边缘模糊半径 (默认: 1.0) |
| preset | String | 否 | 预设模式 (default/portrait/product/hair) |

**响应示例：**
```json
{
  "success": true,
  "prompt_id": "12345-abcde-67890",
  "message": "工作流已提交，请轮询结果"
}
```

### 查询处理结果

```http
GET /api/result/{promptId}
```

**响应示例（处理中）：**
```json
{
  "success": true,
  "status": "processing"
}
```

**响应示例（已完成）：**
```json
{
  "success": true,
  "status": "completed",
  "filename": "ComfyUI_00123.png",
  "subfolder": ""
}
```

### 下载结果图像

```http
GET /api/download/{filename}?subfolder={subfolder}
```

**响应：** 图像文件 (image/png)

## 🛠️ 开发指南

### 构建项目

```bash
# 编译
mvn clean compile

# 运行测试
mvn test

# 打包（生成 JAR）
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests
```

### 运行开发服务器

```bash
# 使用 Maven
mvn spring-boot:run

# 或者在 IDE 中直接运行 MattingApplication.java
```

### 修改前端

前端文件位于 `src/main/resources/static/index.html`

- 无需构建工具
- 修改后刷新浏览器即可看到效果（开发模式下）
- 生产环境需要重新打包

### 添加新功能

1. **添加新的 API 端点**
   - 在 `MattingController.java` 中添加方法

2. **扩展 ComfyUI 服务**
   - 在 `ComfyUIService.java` 中添加方法

3. **修改前端界面**
   - 编辑 `index.html`

## 🚀 部署指南

### 部署到服务器

1. **构建 JAR 文件**
   ```bash
   mvn clean package
   ```

2. **上传到服务器**
   ```bash
   scp target/matting-web-app-1.0.0.jar user@server:/opt/matting-app/
   ```

3. **创建 systemd 服务** (Linux)

   创建 `/etc/systemd/system/matting-app.service`:
   ```ini
   [Unit]
   Description=ComfyUI Matting Web Application
   After=network.target

   [Service]
   Type=simple
   User=www-data
   WorkingDirectory=/opt/matting-app
   ExecStart=/usr/bin/java -jar matting-web-app-1.0.0.jar
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```

4. **启动服务**
   ```bash
   sudo systemctl daemon-reload
   sudo systemctl start matting-app
   sudo systemctl enable matting-app
   ```

### 使用 Docker (可选)

创建 `Dockerfile`:
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/matting-web-app-1.0.0.jar app.jar
COPY sam_mask_matting_api.json .
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行:
```bash
docker build -t matting-web-app .
docker run -p 8080:8080 \
  -e COMFYUI_SERVER_HOST=host.docker.internal \
  matting-web-app
```

### 反向代理 (Nginx)

```nginx
server {
    listen 80;
    server_name matting.example.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 增加上传大小限制
    client_max_body_size 50M;
}
```

## ❓ 常见问题

### Q: 无法连接到 ComfyUI 服务器

**A:** 检查以下几点：
1. ComfyUI 是否正在运行
2. 检查 `application.properties` 中的地址和端口配置
3. 防火墙是否允许连接
4. 如果 ComfyUI 在 Docker 中，使用 `host.docker.internal` 而不是 `localhost`

### Q: 上传文件失败

**A:**
1. 检查文件大小是否超过限制（默认 50MB）
2. 在 `application.properties` 中增加限制：
   ```properties
   spring.servlet.multipart.max-file-size=100MB
   spring.servlet.multipart.max-request-size=100MB
   ```

### Q: 处理一直显示"处理中"

**A:**
1. 检查 ComfyUI 后台是否有错误
2. 查看 Java 应用日志
3. 确保 ComfyUI 安装了所有必需的节点
4. 检查 SAM 模型是否正确加载

### Q: 如何修改端口

**A:** 在 `application.properties` 中修改：
```properties
server.port=9090
```

或使用命令行参数：
```bash
java -jar matting-web-app-1.0.0.jar --server.port=9090
```

### Q: 如何查看日志

**A:**
```bash
# 运行时查看控制台输出

# 或将日志输出到文件
java -jar matting-web-app-1.0.0.jar > app.log 2>&1
```

## 📊 性能优化

### 1. JVM 调优

```bash
java -Xms512m -Xmx2g -XX:+UseG1GC \
  -jar matting-web-app-1.0.0.jar
```

### 2. 连接池配置

在 `application.properties` 中：
```properties
spring.task.execution.pool.core-size=10
spring.task.execution.pool.max-size=20
```

### 3. 启用压缩

```properties
server.compression.enabled=true
server.compression.mime-types=text/html,text/css,application/javascript,application/json
```

## 🔄 从 Gradio 迁移

如果您之前使用的是 Gradio 版本：

1. **无需更改 ComfyUI 工作流**
   - 使用相同的 `sam_mask_matting_api.json`

2. **参数映射**
   - Gradio 的所有参数都已在 Java 版本中实现
   - 预设功能保持一致

3. **启动方式对比**
   ```bash
   # Gradio 版本
   python gradio_app.py

   # Java 版本
   java -jar matting-web-app-1.0.0.jar
   ```

4. **依赖对比**
   - Gradio: 需要 Python + Gradio + 其他依赖 (~500MB)
   - Java: 只需要 Java 运行时 + 应用 JAR (~30MB)

## 📞 技术支持

- 查看项目 README.md
- 查看工作流分析文档：WORKFLOW_ANALYSIS.md
- 查看参数调整指南：NODE_PARAMETERS_GUIDE.md

## 📄 许可证

本项目遵循 MIT 许可证

---

**提示**: 如果您需要更多功能或遇到问题，请参考项目文档或提交 Issue。

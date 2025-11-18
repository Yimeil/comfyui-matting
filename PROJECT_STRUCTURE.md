# ComfyUI Matting Service V2.0 - 项目结构

## 📁 完整目录结构

```
comfyui-matting/
├── .claude/                                    # Claude Skills 定义
│   └── skills/
│       └── matting.md                          # 抠图技能定义
│
├── src/
│   └── main/
│       ├── java/com/yimeil/comfyui/
│       │   ├── ComfyuiMattingApplication.java  # Spring Boot 启动类
│       │   │
│       │   ├── controller/                     # 控制器层
│       │   │   ├── SkillController.java        # Skills API (新架构)
│       │   │   ├── MattingController.java      # 传统 API (向后兼容)
│       │   │   └── PageController.java         # 页面路由
│       │   │
│       │   ├── service/                        # 服务层
│       │   │   ├── SkillExecutor.java          # 技能执行器 (核心)
│       │   │   └── ComfyUIService.java         # ComfyUI API 封装
│       │   │
│       │   ├── model/                          # 数据模型
│       │   │   ├── MattingRequest.java         # 请求模型
│       │   │   ├── MattingResult.java          # 结果模型
│       │   │   └── ApiResponse.java            # 统一响应
│       │   │
│       │   └── config/                         # 配置类
│       │       ├── ComfyUIConfig.java          # ComfyUI 配置
│       │       └── WebConfig.java              # Web 配置
│       │
│       └── resources/
│           ├── application.yml                 # 应用配置
│           ├── static/                         # 静态资源
│           │   └── index.html                  # Vue 3 前端
│           └── workflows/                      # ComfyUI 工作流
│               └── sam_matting.json            # SAM 抠图工作流
│
├── .gitignore                                  # Git 忽略配置
├── pom.xml                                     # Maven 配置 (JDK 21)
├── README.md                                   # 项目主文档
├── ARCHITECTURE.md                             # 架构设计文档
└── PROJECT_STRUCTURE.md                        # 项目结构文档
```

## 🎯 核心文件说明

### 前端 (Vue 3)
- `src/main/resources/static/index.html` - 单页应用，包含完整 UI 和交互逻辑

### 后端 (Spring Boot)

#### 控制器层
- `SkillController.java` - 处理 `/api/skill/*` 请求，基于 Claude Skills 的新架构
- `MattingController.java` - 处理 `/api/matting/*` 请求，传统 API（向后兼容）
- `PageController.java` - 页面路由，转发到 Vue 前端

#### 服务层
- `SkillExecutor.java` - **核心组件**，执行 Claude Skills，验证参数，调用 ComfyUIService
- `ComfyUIService.java` - 封装 ComfyUI API 调用，处理工作流执行

#### 模型层
- `MattingRequest.java` - 抠图请求参数
- `MattingResult.java` - 抠图结果数据
- `ApiResponse.java` - 统一的 API 响应格式

#### 配置层
- `ComfyUIConfig.java` - ComfyUI 相关配置（URL、超时等）
- `WebConfig.java` - Web 配置（CORS、静态资源等）

### Claude Skills
- `.claude/skills/matting.md` - 抠图技能的完整定义文档

### 配置文件
- `application.yml` - Spring Boot 应用配置
- `pom.xml` - Maven 依赖和构建配置

### 文档
- `README.md` - 完整的使用文档和快速开始指南
- `ARCHITECTURE.md` - 详细的架构设计说明

## 🔄 请求流程

```
用户 (浏览器)
    ↓ 上传图片
Vue 3 前端 (index.html)
    ↓ POST /api/skill/matting
SkillController
    ↓ executeMattingSkill()
SkillExecutor
    ↓ 验证 .claude/skills/matting.md
    ↓ 验证参数
    ↓ 调用 ComfyUIService.runMatting()
ComfyUIService
    ↓ 上传图片
    ↓ 加载工作流
    ↓ 执行工作流
    ↓ 下载结果
    ↑ 返回 MattingResult
SkillExecutor
    ↑ 返回结果
SkillController
    ↑ 包装为 ApiResponse
Vue 3 前端
    ↑ 显示结果图片
用户
```

## 📊 技术栈

| 类型 | 技术 | 版本 |
|------|------|------|
| Java | OpenJDK | 21 |
| 框架 | Spring Boot | 3.2.0 |
| 构建工具 | Maven | 3.9+ |
| 前端框架 | Vue | 3.x |
| HTTP 客户端 | Axios | Latest |
| HTTP 库 | Apache HttpClient | 5.3 |

## 🚀 快速启动

```bash
# 1. 确保 ComfyUI 在运行 (http://127.0.0.1:8188)

# 2. 配置 ComfyUI 地址
vim src/main/resources/application.yml

# 3. 启动应用
mvn spring-boot:run

# 4. 访问 http://localhost:8080
```

## 📝 添加新 Skill

1. 在 `.claude/skills/` 创建新的 `.md` 文件定义 skill
2. 在 `SkillExecutor.java` 添加执行方法
3. 在 `SkillController.java` 添加对应的 API 端点
4. 在前端添加 UI 支持

---

**V2.0 - 简洁、模块化、易扩展** 🚀

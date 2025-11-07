# 项目总览 - ComfyUI Matting Service

本项目现在包含**两个版本**，满足不同技术栈需求：

## 📦 版本对比

| 特性 | Python 版本 | Java/Spring Boot 版本 |
|------|------------|---------------------|
| **文档** | README.md | README_JAVA.md |
| **技术栈** | Python 3.8+ | Java 17+ / Spring Boot 3.2 |
| **核心文件** | comfyui_service.py (313行) | ComfyUIService.java + 完整架构 |
| **Web UI** | ❌ 无 | ✅ 完整的 Thymeleaf 界面 |
| **配置文件** | config.yaml | application.yml |
| **API** | Python 函数调用 | RESTful API + Java SDK |
| **运行方式** | `python example.py` | `mvn spring-boot:run` |
| **适用场景** | 快速原型、轻量服务 | 企业级应用、微服务 |

## 🚀 快速启动

### Python 版本

```bash
# 1. 安装依赖
pip install -r requirements.txt

# 2. 配置 ComfyUI 地址
vim config.yaml

# 3. 运行示例
python example.py
```

**3 行代码使用：**
```python
from comfyui_service import ComfyUIService
service = ComfyUIService()
result = service.run_matting("sam_matting.json", "input.jpg")
```

### Java/Spring Boot 版本

```bash
# 1. 配置 ComfyUI 地址
vim src/main/resources/application.yml

# 2. 运行应用
mvn spring-boot:run

# 3. 访问 Web 界面
浏览器打开: http://localhost:8080
```

**Java 代码使用：**
```java
@Autowired
private ComfyUIService comfyUIService;

MattingRequest request = new MattingRequest();
MattingResult result = comfyUIService.runMatting(imageFile, request);
```

## 📁 项目结构

```
comfyui-matting/
├── Python 版本
│   ├── comfyui_service.py          # 核心服务类
│   ├── config.yaml                 # 配置文件
│   ├── example.py                  # 使用示例
│   ├── test_basic.py               # 测试文件
│   ├── requirements.txt            # Python 依赖
│   └── README.md                   # Python 版本文档
│
├── Java/Spring Boot 版本
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/               # Java 源码
│   │   │   │   └── com/yimeil/comfyui/
│   │   │   │       ├── ComfyuiMattingApplication.java
│   │   │   │       ├── controller/ # 控制器
│   │   │   │       ├── service/    # 服务层
│   │   │   │       ├── model/      # 数据模型
│   │   │   │       └── config/     # 配置类
│   │   │   └── resources/
│   │   │       ├── application.yml # 应用配置
│   │   │       ├── workflows/      # 工作流目录
│   │   │       └── templates/      # 前端页面
│   │   └── test/                   # 测试
│   ├── pom.xml                     # Maven 配置
│   └── README_JAVA.md              # Java 版本文档
│
└── 共享资源
    └── workflows/                  # 工作流 JSON 文件
        └── sam_matting.json
```

## 🎯 技术选型建议

### 选择 Python 版本，如果你：
- ✅ 需要快速原型开发
- ✅ 已有 Python 技术栈
- ✅ 需要轻量级微服务
- ✅ 团队熟悉 Python

### 选择 Java/Spring Boot 版本，如果你：
- ✅ 需要企业级应用
- ✅ 已有 Java/Spring Boot 技术栈
- ✅ 需要完整的 Web 管理界面
- ✅ 需要集成到现有 Java 系统
- ✅ 对稳定性和可维护性要求高

## 🔗 核心功能（两个版本均支持）

- ✅ ComfyUI API 封装
- ✅ 工作流加载和执行
- ✅ 图片上传和下载
- ✅ 参数动态配置
- ✅ 抠图结果处理

## 📊 代码统计

### Python 版本
- 核心代码: **313 行** (comfyui_service.py)
- 配置文件: **7 行** (config.yaml)
- 示例代码: **118 行** (example.py)

### Java 版本
- 核心代码: **~400 行** (ComfyUIService.java)
- 控制器: **~80 行**
- 模型类: **~100 行**
- 配置类: **~100 行**
- 前端页面: **~600 行** (index.html)
- 总计: **~1300 行**

## 🌟 参考项目

- [word2picture](https://github.com/treeHeartPig/word2picture) - Spring Boot + ComfyUI 简化架构的灵感来源
- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) - 强大的 Stable Diffusion GUI

## 📝 更新记录

### v1.0.0 (2025-01-XX)
- ✅ Python 极简版本 (参考 word2picture 简化理念)
- ✅ Java/Spring Boot 完整 Web 应用版本
- ✅ 统一的 ComfyUIService 设计
- ✅ SAM 抠图工作流支持
- ✅ 完整的中文文档

---

**两个版本，一个目标：让 ComfyUI API 调用更简单！** 🚀

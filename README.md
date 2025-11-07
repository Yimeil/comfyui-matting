# ComfyUI 多 API 适配器

**通用的 ComfyUI 工作流执行平台** - 支持多种 AI 图像处理任务的统一接口

## 📋 项目概述

这是一个通用的 ComfyUI API 适配器系统，可以轻松集成和管理多个 ComfyUI 工作流。无需修改代码，只需配置文件即可添加新的工作流。

### 核心特性

- 🔌 **通用适配器架构**: 支持任意 ComfyUI 工作流
- ⚙️ **零代码配置**: 通过 YAML 配置文件管理工作流
- 🎨 **动态 Web 界面**: 根据工作流自动生成交互界面
- 📦 **预设管理**: 支持参数预设快速应用
- 🔄 **可扩展设计**: 轻松添加新的工作流类型

## 🏗️ 架构设计

```
ComfyUI 多 API 适配器
├── config/              # 配置文件
│   ├── workflows.yaml  # 工作流注册
│   └── server.yaml     # 服务器配置
├── workflows/          # 工作流模板
│   └── sam_matting/   # SAM 抠图工作流
├── core/               # 核心模块
│   ├── workflow_manager.py
│   ├── workflow_executor.py
│   └── comfyui_client.py
├── adapters/           # 工作流适配器
│   ├── base_adapter.py
│   └── sam_matting_adapter.py
└── ui/                 # Web 界面
    └── app.py
.
├── sam_mask_matting_api.json   # 工作流配置文件（核心）
├── WORKFLOW_ANALYSIS.md        # 工作流深度分析文档
├── NODE_PARAMETERS_GUIDE.md    # 节点参数详细调整指南
│
├── workflow_api_example.py     # Python API 调用示例
│
├── gradio_app.py               # Web 应用（Gradio - Python）
├── WEB_APPLICATION_GUIDE.md    # Gradio Web 应用开发完整指南
├── run_web_app.sh              # Gradio 应用启动脚本
├── requirements.txt            # Python 依赖
│
├── pom.xml                     # Java 项目 Maven 配置
├── run_java_app.sh             # Java 应用启动脚本 (Linux/macOS)
├── run_java_app.bat            # Java 应用启动脚本 (Windows)
├── JAVA_WEB_APP_GUIDE.md       # Java Web 应用完整指南
├── src/                        # Java 源代码目录
│   └── main/
│       ├── java/               # Java 代码
│       └── resources/          # 资源文件（配置、HTML界面）
│
└── README.md                   # 本文件
```

详细架构设计请参考 [MULTI_API_ADAPTER_DESIGN.md](MULTI_API_ADAPTER_DESIGN.md)

## 🚀 快速开始

### 前置要求

1. **ComfyUI 已安装并运行**
   ```bash
   # 启动 ComfyUI (默认端口 8188)
   python main.py
   ```

2. **Python 3.8+**
   ```bash
   python3 --version
   ```

3. **必要的 ComfyUI 自定义节点** (取决于您使用的工作流)
   - 对于 SAM 抠图工作流：
     - `ComfyUI-Impact-Pack`
     - `ComfyUI-SEGS`
     - `comfyui_controlnet_aux`
     - Morphology 节点包

### 安装依赖

```bash
pip install -r requirements.txt
```

### 启动 Web 应用

```bash
# 使用启动脚本（推荐）
./run_web_app.sh

# 或直接运行
python3 -m ui.app
```

然后在浏览器中访问: http://localhost:7860

## 📖 使用指南

### 通过 Web 界面使用

1. **选择工作流**: 从下拉菜单选择需要的处理任务
2. **上传输入**: 根据提示上传图像或其他文件
3. **调整参数**: 使用滑块调整参数，或点击预设快速应用
4. **开始处理**: 点击"开始处理"按钮
5. **查看结果**: 处理完成后在右侧查看结果

### 通过 Python API 使用

```python
from core.workflow_executor import WorkflowExecutor

# 初始化执行器
executor = WorkflowExecutor("127.0.0.1:8188")

# 列出所有可用工作流
workflows = executor.list_workflows()
print(workflows)

# 执行工作流
result = executor.execute(
    workflow_id="sam_matting",
    inputs={
        "image": "input.jpg",
        "mask": "mask.png"
    },
    params={
        "mask_threshold": 0.7,
        "blur_radius": 2.0
    }
)

# 保存结果
if result['success'] and result['downloaded_images']:
    result['downloaded_images'][0]['image'].save("result.png")
```

### 使用预设

```python
# 使用预设配置执行
result = executor.execute_with_preset(
    workflow_id="sam_matting",
    inputs={
        "image": "input.jpg",
        "mask": "mask.png"
    },
    preset_name="portrait"  # 人像模式
)
#### 方式 D: 使用 Java + HTML Web 应用（轻量级，推荐）

**🎯 最轻量级的 Web 方案！** 无需安装大量 Python 依赖，只需要 Java 运行环境。

```bash
# 1. 确保安装了 Java 17+
java -version

# 2. 启动 Web 应用
./run_java_app.sh      # Linux/macOS
# 或
run_java_app.bat       # Windows

# 3. 在浏览器中访问
# http://localhost:8080
```

**Java Web 应用特性：**
- ✅ **零 Python 依赖** - 只需要 Java 运行环境
- ✅ **轻量级部署** - 单个 JAR 文件，约 30MB
- ✅ **高性能** - Spring Boot 提供的企业级性能
- ✅ **现代化界面** - 响应式设计，支持移动端
- ✅ **易于维护** - 跨平台，易于部署和扩展
- 🖱️ 拖拽上传图像和蒙版
- 🎛️ 可视化参数调节
- 🎨 实时预览结果
- 📥 一键下载结果
- 🚀 快速预设（人像/产品/毛发模式）

**详细指南：** 查看 `JAVA_WEB_APP_GUIDE.md` 了解：
- 安装和配置
- API 接口文档
- 部署到生产环境
- 自定义开发

## 🎨 工作流程

```
输入图像 + 蒙版提示
    ↓
SAM 智能分割
    ↓
形态学闭运算 (填充孔洞)
    ↓
收缩 + 模糊 (边缘羽化)
    ↓
应用到原图
    ↓
输出抠图结果
```

## 🔧 添加新工作流

只需 4 步即可添加新的工作流：

### 1. 创建工作流目录

```bash
mkdir -p workflows/my_workflow
```

### 2. 导出 ComfyUI 工作流

在 ComfyUI 中：
- 构建您的工作流
- 点击 "Save (API Format)"
- 保存为 `workflows/my_workflow/workflow.json`

### 3. 创建参数定义

创建 `workflows/my_workflow/schema.yaml`:

```yaml
workflow_id: my_workflow
version: "1.0.0"
name: "我的工作流"
description: "工作流描述"

inputs:
  - name: input_image
    type: image
    required: true
    label: "输入图像"
    node_id: "1"
    node_param: "image"

parameters:
  - name: strength
    type: float
    label: "强度"
    default: 0.5
    min: 0.0
    max: 1.0
    step: 0.1
    node_id: "3"
    node_param: "denoise"
    category: "基础参数"

presets:
  default:
    name: "默认"
    icon: "⚡"
    params:
      strength: 0.5

outputs:
  - name: result
    type: image
    node_id: "9"
```

### 4. 创建适配器并注册

创建 `adapters/my_workflow_adapter.py`:

```python
from adapters.base_adapter import BaseAdapter

class MyWorkflowAdapter(BaseAdapter):
    def validate_inputs(self, inputs):
        if 'input_image' not in inputs:
            raise ValueError("缺少输入图像")
        return True

    def prepare_workflow(self, workflow, inputs, params):
        workflow['1']['inputs']['image'] = inputs['input_image']
        workflow['3']['inputs']['denoise'] = params.get('strength', 0.5)
        return workflow

    def process_outputs(self, outputs):
        return {
            'success': True,
            'images': outputs.get('9', {}).get('images', [])
        }
```

在 `config/workflows.yaml` 注册:

```yaml
workflows:
  my_workflow:
    name: "我的工作流"
    description: "工作流描述"
    adapter: "adapters.my_workflow_adapter.MyWorkflowAdapter"
    workflow_file: "workflows/my_workflow/workflow.json"
    schema_file: "workflows/my_workflow/schema.yaml"
    enabled: true
    icon: "✨"
    category: "自定义"
```

重启应用即可看到新工作流！

## 📦 内置工作流

### SAM 智能抠图 (sam_matting)

使用 Segment Anything Model 进行智能图像抠图。

**输入:**
- 原始图像
- 蒙版图像

**参数:**
- 检测阈值 (0.1-1.0)
- 边缘模糊 (0-5)
- 形态学核大小 (2-15)
- 蒙版扩展 (-10 到 10)

**预设:**
- 👤 人像模式
- 📦 产品模式
- 💇 毛发模式
- ⭐ 高质量
- ⚡ 快速模式

详细说明请参考:
- [工作流分析](WORKFLOW_ANALYSIS.md)
- [参数调优指南](NODE_PARAMETERS_GUIDE.md)

## 🛠️ 配置

### 服务器配置 (config/server.yaml)

```yaml
server:
  comfyui_url: "127.0.0.1:8188"  # ComfyUI 服务器地址
  web_port: 7860                  # Web 界面端口
  web_host: "0.0.0.0"            # Web 界面地址
  share: false                    # 是否创建公共链接
  max_file_size: 10               # 最大文件大小 (MB)
  execution_timeout: 300          # 执行超时 (秒)
```

### 工作流配置 (config/workflows.yaml)

所有工作流通过此文件注册和管理。

## 📚 文档

- [架构设计文档](MULTI_API_ADAPTER_DESIGN.md) - 详细的系统架构说明
- [工作流分析](WORKFLOW_ANALYSIS.md) - SAM 抠图工作流深度分析
- [参数调优指南](NODE_PARAMETERS_GUIDE.md) - 节点参数详细说明
- [Web 应用指南](WEB_APPLICATION_GUIDE.md) - Web 应用开发指南

## 🔍 故障排查

### ComfyUI 连接失败

```bash
# 检查 ComfyUI 是否运行
curl http://127.0.0.1:8188/system_stats

# 如果 ComfyUI 在其他端口，修改 config/server.yaml
```

### 工作流未显示

1. 检查 `config/workflows.yaml` 中是否启用: `enabled: true`
2. 检查工作流文件路径是否正确
3. 查看终端输出的错误信息

### 缺少自定义节点

根据工作流要求安装相应的 ComfyUI 自定义节点包。

## 🤝 贡献

欢迎贡献新的工作流适配器！

1. Fork 项目
2. 创建工作流适配器
3. 提交 Pull Request

## 📄 许可

MIT License

## 🌟 相关项目

- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) - 强大的 Stable Diffusion GUI
- [Segment Anything](https://github.com/facebookresearch/segment-anything) - Meta 的通用分割模型

---

**提示**: 这是一个通用框架，您可以基于它构建任何 ComfyUI 工作流的 API 服务！

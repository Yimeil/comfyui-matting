# ComfyUI Matting Service

**简单易用的 ComfyUI 抠图服务** - 参考 [word2picture](https://github.com/treeHeartPig/word2picture) 的简化架构

## 📋 项目概述

这是一个极简的 ComfyUI API 服务封装，通过简单的配置即可调用 ComfyUI 工作流进行图像抠图处理。

### 核心特性

- 🚀 **极简架构**: 单文件服务类，无复杂依赖
- ⚙️ **简单配置**: 只需配置 ComfyUI 服务器地址
- 📦 **开箱即用**: 3 行代码即可完成抠图
- 🔄 **灵活扩展**: 支持任意 ComfyUI 工作流

## 🏗️ 项目结构

```
comfyui-matting/
├── comfyui_service.py  # 核心服务类（单文件）
├── config.yaml         # 配置文件（只配置服务器地址）
├── workflows/          # 工作流 JSON 文件目录
│   └── sam_matting.json
├── example.py          # 使用示例
└── requirements.txt    # Python 依赖
```

**对比传统架构的优势：**
- ❌ 无需复杂的适配器系统
- ❌ 无需 Schema 验证
- ❌ 无需多层抽象
- ✅ 直接调用，简单明了

## 🚀 快速开始

### 1. 前置要求

- **ComfyUI 已安装并运行** (默认端口 8188)
  ```bash
  # 启动 ComfyUI
  python main.py
  ```

- **Python 3.8+**

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置服务器地址

编辑 `config.yaml`:

```yaml
# ComfyUI API 地址（必须配置）
comfyui_api_url: "127.0.0.1:8188"

# 工作流文件目录
workflows_dir: "workflows"

# 请求超时时间（秒）
timeout: 30
```

### 4. 开始使用

**最简单的用法（3 行代码）：**

```python
from comfyui_service import ComfyUIService

service = ComfyUIService()
result = service.run_matting("sam_matting.json", "input.jpg")
```

就这么简单！ 🎉

## 📖 使用示例

### 示例 1: 一键抠图（默认参数）

```python
from comfyui_service import ComfyUIService

# 初始化服务
service = ComfyUIService()

# 检查服务器
if not service.check_server():
    print("无法连接到 ComfyUI 服务器")
    exit(1)

# 执行抠图
result = service.run_matting(
    workflow_name="sam_matting.json",
    input_image="test.jpg",
    output_dir="output"
)

print(f"完成！结果: {result}")
```

### 示例 2: 自定义参数

```python
# 自定义参数（节点ID: {参数名: 参数值}）
params = {
    "15": {  # SAM 模型节点
        "threshold": 0.5
    },
    "23": {  # Alpha Matting 节点
        "alpha_matting": "true",
        "alpha_matting_foreground_threshold": 240,
        "alpha_matting_background_threshold": 10
    }
}

result = service.run_matting(
    workflow_name="sam_matting.json",
    input_image="test.jpg",
    params=params,
    output_dir="output"
)
```

### 示例 3: 完全控制（底层 API）

```python
# 1. 加载工作流
workflow = service.load_workflow("sam_matting.json")

# 2. 上传图片
uploaded_name = service.upload_image("test.jpg")

# 3. 更新工作流参数
workflow = service.update_workflow_params(workflow, "10", "image", uploaded_name)
workflow = service.update_workflow_params(workflow, "15", "threshold", 0.5)

# 4. 执行工作流
outputs = service.execute_workflow(workflow)

# 5. 下载结果
for node_id, node_output in outputs.items():
    if 'images' in node_output:
        for img in node_output['images']:
            service.download_image(
                filename=img['filename'],
                output_path=f"output/{img['filename']}",
                subfolder=img.get('subfolder', '')
            )
```

更多示例请查看 `example.py`

## 🔧 添加新工作流

只需 2 步：

### 1. 导出 ComfyUI 工作流

在 ComfyUI 中：
- 构建您的工作流
- 点击 "Save (API Format)"
- 保存到 `workflows/your_workflow.json`

### 2. 使用工作流

```python
result = service.run_matting("your_workflow.json", "input.jpg")
```

就这么简单！无需写适配器，无需写配置。

## 📚 API 文档

### ComfyUIService 类

#### 初始化

```python
service = ComfyUIService(config_path="config.yaml")
```

#### 主要方法

**一键执行（推荐）：**

```python
run_matting(workflow_name, input_image, params=None, output_dir="output", verbose=True)
```

**底层方法：**

- `load_workflow(workflow_name)` - 加载工作流 JSON
- `upload_image(image_path)` - 上传图片
- `update_workflow_params(workflow, node_id, param_name, param_value)` - 更新参数
- `execute_workflow(workflow, verbose=True)` - 执行工作流
- `download_image(filename, output_path, subfolder="", folder_type="output")` - 下载图片
- `check_server()` - 检查服务器状态

## 🎨 内置工作流

### SAM 智能抠图 (sam_matting.json)

使用 Segment Anything Model 进行智能图像抠图。

**输入：** 图片文件路径

**输出：** 抠图后的 PNG 图片（带透明背景）

**关键节点参数：**
- 节点 10: 图片输入
- 节点 15: SAM 阈值 (threshold)
- 节点 23: Alpha Matting 参数

## 🔍 故障排查

### 无法连接 ComfyUI

```bash
# 检查 ComfyUI 是否运行
curl http://127.0.0.1:8188/system_stats

# 如果在其他端口，修改 config.yaml 中的 comfyui_api_url
```

### 工作流文件未找到

确保工作流 JSON 文件在 `workflows/` 目录下。

### 执行失败

1. 检查 ComfyUI 是否安装了所需的自定义节点
2. 查看终端输出的详细错误信息
3. 确认工作流 JSON 格式正确（API Format）

## 🌟 为什么选择简化架构？

| 传统架构 | 简化架构 |
|---------|---------|
| 4 层抽象（Adapter → Manager → Executor → Client） | 1 层服务（Service） |
| 893+ 行核心代码 | 300+ 行核心代码 |
| 需要 YAML Schema 验证 | 直接使用工作流 JSON |
| 需要写适配器类 | 无需额外代码 |
| 学习曲线陡峭 | 3 行代码上手 |

**参考项目：** [word2picture](https://github.com/treeHeartPig/word2picture) - 简单实用的 ComfyUI Java 封装

## 📄 许可

MIT License

## 🔗 相关链接

- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) - 强大的 Stable Diffusion GUI
- [word2picture](https://github.com/treeHeartPig/word2picture) - 参考的简化架构
- [Segment Anything](https://github.com/facebookresearch/segment-anything) - Meta 的通用分割模型

---

**Keep It Simple!** 🚀

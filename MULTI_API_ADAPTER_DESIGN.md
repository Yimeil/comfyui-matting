# ComfyUI 多 API 适配器架构设计

## 项目概述

将现有的 ComfyUI Matting 项目改造成一个通用的多 API 适配器系统，支持多种 ComfyUI 工作流的动态加载、配置和执行。

## 架构目标

1. **通用性**：支持任意 ComfyUI 工作流的加载和执行
2. **可扩展性**：轻松添加新的工作流而无需修改代码
3. **灵活性**：动态配置参数和预设
4. **易用性**：简洁的 Web 界面和 API
5. **可维护性**：清晰的代码结构和配置管理

## 新的项目结构

```
comfyui-adapter/
├── config/                          # 配置文件目录
│   ├── workflows.yaml              # 工作流注册配置
│   ├── presets.yaml                # 参数预设配置
│   └── server.yaml                 # 服务器配置
│
├── workflows/                       # 工作流模板目录
│   ├── sam_matting/
│   │   ├── workflow.json           # ComfyUI 工作流
│   │   ├── schema.yaml             # 参数定义
│   │   └── readme.md               # 工作流说明
│   ├── style_transfer/
│   │   ├── workflow.json
│   │   ├── schema.yaml
│   │   └── readme.md
│   └── image_upscale/
│       ├── workflow.json
│       ├── schema.yaml
│       └── readme.md
│
├── core/                            # 核心模块
│   ├── __init__.py
│   ├── workflow_executor.py        # 工作流执行器
│   ├── workflow_manager.py         # 工作流管理器
│   ├── parameter_manager.py        # 参数管理器
│   ├── preset_manager.py           # 预设管理器
│   └── comfyui_client.py           # ComfyUI 客户端
│
├── adapters/                        # 适配器目录
│   ├── __init__.py
│   ├── base_adapter.py             # 基础适配器
│   ├── sam_matting_adapter.py      # SAM 抠图适配器
│   ├── style_transfer_adapter.py   # 风格迁移适配器
│   └── image_upscale_adapter.py    # 图像放大适配器
│
├── ui/                              # 用户界面
│   ├── __init__.py
│   ├── app.py                      # 主应用入口
│   ├── dynamic_ui.py               # 动态 UI 生成器
│   └── components/                 # UI 组件
│       ├── workflow_selector.py    # 工作流选择器
│       ├── parameter_panel.py      # 参数面板
│       └── result_viewer.py        # 结果查看器
│
├── api/                             # API 接口
│   ├── __init__.py
│   ├── rest_api.py                 # REST API
│   └── schemas.py                  # API 数据模型
│
├── utils/                           # 工具函数
│   ├── __init__.py
│   ├── config_loader.py            # 配置加载器
│   ├── image_utils.py              # 图像处理工具
│   └── validation.py               # 数据验证
│
├── tests/                           # 测试
│   ├── test_workflow_executor.py
│   ├── test_adapters.py
│   └── test_api.py
│
├── run_web_app.sh                   # Web 应用启动脚本
├── requirements.txt                 # 依赖
└── README.md                        # 项目文档
```

## 核心组件设计

### 1. 工作流注册系统

**config/workflows.yaml**
```yaml
workflows:
  sam_matting:
    name: "SAM 智能抠图"
    description: "使用 Segment Anything Model 进行智能图像抠图"
    adapter: "adapters.sam_matting_adapter.SAMMattingAdapter"
    workflow_file: "workflows/sam_matting/workflow.json"
    schema_file: "workflows/sam_matting/schema.yaml"
    enabled: true
    icon: "✂️"
    category: "图像处理"

  style_transfer:
    name: "风格迁移"
    description: "将艺术风格应用到图像上"
    adapter: "adapters.style_transfer_adapter.StyleTransferAdapter"
    workflow_file: "workflows/style_transfer/workflow.json"
    schema_file: "workflows/style_transfer/schema.yaml"
    enabled: true
    icon: "🎨"
    category: "图像生成"

  image_upscale:
    name: "图像超分辨率"
    description: "使用 AI 提高图像分辨率"
    adapter: "adapters.image_upscale_adapter.ImageUpscaleAdapter"
    workflow_file: "workflows/image_upscale/workflow.json"
    schema_file: "workflows/image_upscale/schema.yaml"
    enabled: true
    icon: "🔍"
    category: "图像增强"
```

### 2. 参数定义系统

**workflows/sam_matting/schema.yaml**
```yaml
workflow_id: sam_matting
version: "1.0.0"

inputs:
  - name: image
    type: image
    required: true
    label: "原始图像"
    description: "需要抠图的图像"
    accept: [".jpg", ".png", ".jpeg"]

  - name: mask
    type: image
    required: true
    label: "蒙版图像"
    description: "指示需要保留的区域"
    accept: [".png"]

parameters:
  - name: mask_threshold
    type: float
    label: "检测阈值"
    description: "SAM 模型检测的置信度阈值"
    default: 0.6
    min: 0.1
    max: 1.0
    step: 0.1
    node_id: "10"
    node_param: "mask_hint_threshold"

  - name: blur_radius
    type: float
    label: "边缘模糊"
    description: "边缘羽化半径"
    default: 1.0
    min: 0.0
    max: 5.0
    step: 0.1
    node_id: "23"
    node_param: "blur_radius"

  - name: kernel_size
    type: int
    label: "形态学核大小"
    description: "用于填充孔洞的核大小"
    default: 6
    min: 2
    max: 15
    step: 1
    node_id: "43"
    node_param: "kernel_size"

  - name: expand
    type: int
    label: "蒙版扩展"
    description: "正值扩展蒙版，负值收缩蒙版"
    default: -3
    min: -10
    max: 10
    step: 1
    node_id: "23"
    node_param: "expand"

presets:
  portrait:
    name: "人像模式"
    icon: "👤"
    description: "适合人像抠图，保留柔和边缘"
    params:
      mask_threshold: 0.7
      blur_radius: 2.5
      kernel_size: 6
      expand: -4

  product:
    name: "产品模式"
    icon: "📦"
    description: "适合产品图抠图，边缘清晰锐利"
    params:
      mask_threshold: 0.6
      blur_radius: 0.3
      kernel_size: 10
      expand: -1

  hair:
    name: "毛发模式"
    icon: "💇"
    description: "适合有复杂毛发的对象"
    params:
      mask_threshold: 0.6
      blur_radius: 1.5
      kernel_size: 6
      expand: -1

outputs:
  - name: result
    type: image
    label: "抠图结果"
    description: "带透明背景的抠图结果"
    node_id: "22"
```

### 3. 基础适配器类

**adapters/base_adapter.py**
```python
from abc import ABC, abstractmethod
from typing import Dict, Any, List
import yaml

class BaseAdapter(ABC):
    """工作流适配器基类"""

    def __init__(self, workflow_config: Dict[str, Any]):
        self.workflow_id = workflow_config['workflow_id']
        self.name = workflow_config['name']
        self.description = workflow_config['description']
        self.workflow_file = workflow_config['workflow_file']
        self.schema_file = workflow_config['schema_file']
        self.schema = self._load_schema()

    def _load_schema(self) -> Dict[str, Any]:
        """加载工作流参数定义"""
        with open(self.schema_file, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f)

    @abstractmethod
    def validate_inputs(self, inputs: Dict[str, Any]) -> bool:
        """验证输入参数"""
        pass

    @abstractmethod
    def prepare_workflow(self, workflow: Dict, inputs: Dict, params: Dict) -> Dict:
        """准备工作流（更新参数和输入）"""
        pass

    @abstractmethod
    def process_outputs(self, outputs: Dict) -> Dict:
        """处理输出结果"""
        pass

    def get_parameter_info(self) -> List[Dict]:
        """获取参数信息"""
        return self.schema.get('parameters', [])

    def get_presets(self) -> Dict[str, Dict]:
        """获取预设配置"""
        return self.schema.get('presets', {})

    def get_input_schema(self) -> List[Dict]:
        """获取输入定义"""
        return self.schema.get('inputs', [])

    def apply_preset(self, preset_name: str) -> Dict[str, Any]:
        """应用预设配置"""
        presets = self.get_presets()
        if preset_name in presets:
            return presets[preset_name]['params']
        return {}
```

### 4. SAM 抠图适配器

**adapters/sam_matting_adapter.py**
```python
from typing import Dict, Any
from .base_adapter import BaseAdapter

class SAMMattingAdapter(BaseAdapter):
    """SAM 抠图适配器"""

    def validate_inputs(self, inputs: Dict[str, Any]) -> bool:
        """验证输入"""
        required_inputs = ['image', 'mask']
        for inp in required_inputs:
            if inp not in inputs or not inputs[inp]:
                raise ValueError(f"缺少必需输入: {inp}")
        return True

    def prepare_workflow(self, workflow: Dict, inputs: Dict, params: Dict) -> Dict:
        """准备工作流"""
        # 更新图像输入
        workflow['2']['inputs']['image'] = inputs['image']
        workflow['2']['inputs']['mask'] = inputs['mask']

        # 更新参数
        for param_name, param_value in params.items():
            param_info = self._get_parameter_by_name(param_name)
            if param_info:
                node_id = param_info['node_id']
                node_param = param_info['node_param']
                workflow[node_id]['inputs'][node_param] = param_value

        return workflow

    def process_outputs(self, outputs: Dict) -> Dict:
        """处理输出"""
        # 从 ComfyUI 历史记录中提取结果图像
        result = {
            'success': True,
            'images': [],
            'message': '抠图完成'
        }

        if '22' in outputs:  # PreviewImage 节点
            node_output = outputs['22']
            if 'images' in node_output:
                result['images'] = node_output['images']

        return result

    def _get_parameter_by_name(self, name: str) -> Dict:
        """根据名称获取参数信息"""
        for param in self.schema['parameters']:
            if param['name'] == name:
                return param
        return None
```

### 5. 工作流管理器

**core/workflow_manager.py**
```python
import yaml
import importlib
from typing import Dict, List, Any
from pathlib import Path

class WorkflowManager:
    """工作流管理器"""

    def __init__(self, config_path: str = "config/workflows.yaml"):
        self.config_path = config_path
        self.workflows = {}
        self.adapters = {}
        self._load_workflows()

    def _load_workflows(self):
        """加载工作流配置"""
        with open(self.config_path, 'r', encoding='utf-8') as f:
            config = yaml.safe_load(f)

        for workflow_id, workflow_config in config['workflows'].items():
            if workflow_config.get('enabled', True):
                workflow_config['workflow_id'] = workflow_id
                self.workflows[workflow_id] = workflow_config

                # 动态加载适配器
                adapter_class = self._load_adapter_class(workflow_config['adapter'])
                self.adapters[workflow_id] = adapter_class(workflow_config)

    def _load_adapter_class(self, adapter_path: str):
        """动态加载适配器类"""
        module_path, class_name = adapter_path.rsplit('.', 1)
        module = importlib.import_module(module_path)
        return getattr(module, class_name)

    def get_workflow(self, workflow_id: str) -> Dict:
        """获取工作流配置"""
        return self.workflows.get(workflow_id)

    def get_adapter(self, workflow_id: str):
        """获取工作流适配器"""
        return self.adapters.get(workflow_id)

    def list_workflows(self) -> List[Dict]:
        """列出所有可用的工作流"""
        result = []
        for workflow_id, config in self.workflows.items():
            result.append({
                'id': workflow_id,
                'name': config['name'],
                'description': config['description'],
                'icon': config.get('icon', '⚙️'),
                'category': config.get('category', '其他')
            })
        return result

    def get_workflow_schema(self, workflow_id: str) -> Dict:
        """获取工作流的参数定义"""
        adapter = self.get_adapter(workflow_id)
        if adapter:
            return adapter.schema
        return {}
```

### 6. 工作流执行器

**core/workflow_executor.py**
```python
import json
from typing import Dict, Any
from .comfyui_client import ComfyUIClient
from .workflow_manager import WorkflowManager

class WorkflowExecutor:
    """工作流执行器"""

    def __init__(self, comfyui_url: str = "127.0.0.1:8188"):
        self.client = ComfyUIClient(comfyui_url)
        self.workflow_manager = WorkflowManager()

    def execute(self, workflow_id: str, inputs: Dict[str, Any],
                params: Dict[str, Any] = None) -> Dict:
        """执行工作流"""
        # 获取适配器
        adapter = self.workflow_manager.get_adapter(workflow_id)
        if not adapter:
            raise ValueError(f"未找到工作流: {workflow_id}")

        # 验证输入
        adapter.validate_inputs(inputs)

        # 加载工作流模板
        workflow_config = self.workflow_manager.get_workflow(workflow_id)
        with open(workflow_config['workflow_file'], 'r') as f:
            workflow = json.load(f)

        # 准备工作流
        params = params or {}
        workflow = adapter.prepare_workflow(workflow, inputs, params)

        # 提交执行
        result = self.client.queue_prompt(workflow)
        prompt_id = result['prompt_id']

        # 等待完成
        self.client.track_progress(prompt_id)

        # 获取结果
        history = self.client.get_history(prompt_id)
        outputs = history[prompt_id]['outputs']

        # 处理输出
        processed_result = adapter.process_outputs(outputs)

        return processed_result

    def list_workflows(self) -> List[Dict]:
        """列出所有工作流"""
        return self.workflow_manager.list_workflows()

    def get_workflow_info(self, workflow_id: str) -> Dict:
        """获取工作流详细信息"""
        schema = self.workflow_manager.get_workflow_schema(workflow_id)
        config = self.workflow_manager.get_workflow(workflow_id)

        return {
            'id': workflow_id,
            'name': config['name'],
            'description': config['description'],
            'icon': config.get('icon', '⚙️'),
            'category': config.get('category', '其他'),
            'inputs': schema.get('inputs', []),
            'parameters': schema.get('parameters', []),
            'presets': schema.get('presets', {})
        }
```

### 7. 动态 Web UI

**ui/app.py**
```python
import gradio as gr
from core.workflow_executor import WorkflowExecutor
from .dynamic_ui import DynamicUI

class MultiAPIApp:
    """多 API 适配器 Web 应用"""

    def __init__(self, comfyui_url: str = "127.0.0.1:8188"):
        self.executor = WorkflowExecutor(comfyui_url)
        self.ui_builder = DynamicUI(self.executor)

    def launch(self, server_port: int = 7860, share: bool = False):
        """启动 Web 应用"""
        app = self.ui_builder.build_interface()
        app.launch(
            server_port=server_port,
            share=share,
            server_name="0.0.0.0"
        )

if __name__ == "__main__":
    app = MultiAPIApp()
    app.launch()
```

**ui/dynamic_ui.py**
```python
import gradio as gr
from typing import Dict, Any, List

class DynamicUI:
    """动态 UI 构建器"""

    def __init__(self, executor):
        self.executor = executor

    def build_interface(self):
        """构建动态界面"""
        workflows = self.executor.list_workflows()

        with gr.Blocks(title="ComfyUI 多 API 适配器", theme=gr.themes.Soft()) as app:
            gr.Markdown("# 🎨 ComfyUI 多 API 适配器")
            gr.Markdown("支持多种 AI 图像处理工作流的通用平台")

            # 工作流选择
            workflow_choices = [f"{w['icon']} {w['name']}" for w in workflows]
            workflow_ids = [w['id'] for w in workflows]

            workflow_selector = gr.Dropdown(
                choices=workflow_choices,
                label="选择工作流",
                value=workflow_choices[0] if workflow_choices else None
            )

            # 动态内容区域
            with gr.Row():
                with gr.Column(scale=1):
                    input_components = gr.Column(visible=True)
                    param_components = gr.Column(visible=True)

                with gr.Column(scale=1):
                    output_components = gr.Column(visible=True)

            # 工作流切换时动态更新 UI
            workflow_selector.change(
                fn=self._build_workflow_ui,
                inputs=[workflow_selector],
                outputs=[input_components, param_components, output_components]
            )

            # 初始化第一个工作流的 UI
            if workflow_ids:
                self._build_workflow_ui(workflow_choices[0])

        return app

    def _build_workflow_ui(self, selected_workflow: str):
        """根据选择的工作流构建 UI"""
        # 解析工作流 ID
        workflow_id = None
        workflows = self.executor.list_workflows()
        for w in workflows:
            if f"{w['icon']} {w['name']}" == selected_workflow:
                workflow_id = w['id']
                break

        if not workflow_id:
            return None, None, None

        # 获取工作流信息
        info = self.executor.get_workflow_info(workflow_id)

        # 构建输入组件
        input_ui = self._build_input_components(info['inputs'])

        # 构建参数组件
        param_ui = self._build_parameter_components(info['parameters'], info['presets'])

        # 构建输出组件
        output_ui = self._build_output_components()

        return input_ui, param_ui, output_ui

    def _build_input_components(self, inputs: List[Dict]):
        """构建输入组件"""
        components = []
        for inp in inputs:
            if inp['type'] == 'image':
                comp = gr.Image(
                    label=inp['label'],
                    type="filepath"
                )
                components.append(comp)
        return components

    def _build_parameter_components(self, parameters: List[Dict], presets: Dict):
        """构建参数组件"""
        components = []

        for param in parameters:
            if param['type'] == 'float':
                comp = gr.Slider(
                    minimum=param['min'],
                    maximum=param['max'],
                    value=param['default'],
                    step=param['step'],
                    label=param['label'],
                    info=param.get('description', '')
                )
            elif param['type'] == 'int':
                comp = gr.Slider(
                    minimum=param['min'],
                    maximum=param['max'],
                    value=param['default'],
                    step=param['step'],
                    label=param['label'],
                    info=param.get('description', '')
                )
            components.append(comp)

        # 添加预设按钮
        if presets:
            preset_buttons = []
            for preset_id, preset_info in presets.items():
                btn = gr.Button(f"{preset_info['icon']} {preset_info['name']}")
                preset_buttons.append(btn)
            components.extend(preset_buttons)

        return components

    def _build_output_components(self):
        """构建输出组件"""
        return [
            gr.Image(label="处理结果", type="filepath"),
            gr.Textbox(label="状态信息", lines=3)
        ]
```

## 使用示例

### 添加新工作流

1. **创建工作流目录**
```bash
mkdir -p workflows/my_workflow
```

2. **放置 ComfyUI 工作流文件**
```bash
# 从 ComfyUI 导出 API 格式的工作流
cp my_workflow_api.json workflows/my_workflow/workflow.json
```

3. **创建参数定义**
```yaml
# workflows/my_workflow/schema.yaml
workflow_id: my_workflow
version: "1.0.0"

inputs:
  - name: input_image
    type: image
    required: true
    label: "输入图像"

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

presets:
  default:
    name: "默认"
    params:
      strength: 0.5

outputs:
  - name: result
    type: image
    label: "输出图像"
    node_id: "9"
```

4. **创建适配器**
```python
# adapters/my_workflow_adapter.py
from .base_adapter import BaseAdapter

class MyWorkflowAdapter(BaseAdapter):
    def validate_inputs(self, inputs):
        if 'input_image' not in inputs:
            raise ValueError("缺少输入图像")
        return True

    def prepare_workflow(self, workflow, inputs, params):
        workflow['3']['inputs']['image'] = inputs['input_image']
        workflow['3']['inputs']['denoise'] = params.get('strength', 0.5)
        return workflow

    def process_outputs(self, outputs):
        return {
            'success': True,
            'images': outputs.get('9', {}).get('images', [])
        }
```

5. **注册工作流**
```yaml
# config/workflows.yaml
workflows:
  my_workflow:
    name: "我的工作流"
    description: "自定义工作流描述"
    adapter: "adapters.my_workflow_adapter.MyWorkflowAdapter"
    workflow_file: "workflows/my_workflow/workflow.json"
    schema_file: "workflows/my_workflow/schema.yaml"
    enabled: true
    icon: "✨"
    category: "自定义"
```

### API 使用

```python
from core.workflow_executor import WorkflowExecutor

# 初始化执行器
executor = WorkflowExecutor("127.0.0.1:8188")

# 列出所有工作流
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

print(result)
```

## 优势

1. **零代码添加工作流**：只需配置文件，无需修改代码
2. **统一的接口**：所有工作流使用相同的调用方式
3. **动态 UI 生成**：根据配置自动生成 Web 界面
4. **参数验证**：自动验证输入和参数
5. **预设管理**：支持多种参数预设
6. **易于扩展**：清晰的适配器模式

## 下一步

1. 实现核心模块代码
2. 迁移现有的 SAM 抠图工作流
3. 添加更多示例工作流
4. 编写测试用例
5. 完善文档

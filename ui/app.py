"""
多 API 适配器 Web 应用
"""

import gradio as gr
import yaml
import os
import sys

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from core.workflow_executor import WorkflowExecutor


class MultiAPIApp:
    """多 API 适配器 Web 应用"""

    def __init__(self, config_path: str = "config/server.yaml"):
        """
        初始化应用

        Args:
            config_path: 服务器配置文件路径
        """
        # 加载服务器配置
        self.config = self._load_config(config_path)

        # 初始化执行器
        comfyui_url = self.config.get('server', {}).get('comfyui_url', '127.0.0.1:8188')
        self.executor = WorkflowExecutor(comfyui_url)

        # 检查服务器状态
        self._check_server()

    def _load_config(self, config_path: str) -> dict:
        """加载配置文件"""
        if os.path.exists(config_path):
            with open(config_path, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f) or {}
        return {}

    def _check_server(self):
        """检查 ComfyUI 服务器状态"""
        if not self.executor.check_server():
            print("⚠️  警告: 无法连接到 ComfyUI 服务器")
            print(f"   请确保 ComfyUI 正在运行: {self.executor.client.server_address}")

    def build_interface(self) -> gr.Blocks:
        """构建 Gradio 界面"""
        # 获取所有工作流
        workflows = self.executor.list_workflows()

        if not workflows:
            return self._build_empty_interface()

        # 按类别分组工作流
        categories = {}
        for wf in workflows:
            category = wf.get('category', '其他')
            if category not in categories:
                categories[category] = []
            categories[category].append(wf)

        # 构建界面
        with gr.Blocks(
            title="ComfyUI 多 API 适配器",
            theme=gr.themes.Soft()
        ) as app:
            # 标题
            gr.Markdown("# 🎨 ComfyUI 多 API 适配器")
            gr.Markdown("通用的 ComfyUI 工作流执行平台 - 支持多种 AI 图像处理任务")

            # 服务器状态
            with gr.Accordion("📡 服务器状态", open=False):
                server_status = gr.Markdown(self._get_server_status())
                refresh_btn = gr.Button("🔄 刷新状态", size="sm")

            # 工作流选择
            workflow_dropdown = gr.Dropdown(
                choices=[f"{wf['icon']} {wf['name']}" for wf in workflows],
                label="选择工作流",
                value=f"{workflows[0]['icon']} {workflows[0]['name']}",
                interactive=True
            )

            # 工作流描述
            workflow_desc = gr.Markdown(f"**描述**: {workflows[0]['description']}")

            gr.Markdown("---")

            # 动态内容区域
            with gr.Row():
                with gr.Column(scale=1):
                    # 输入区域
                    inputs_column = gr.Column()

                    # 参数区域
                    params_column = gr.Column()

                    # 预设按钮区域
                    presets_column = gr.Column()

                    # 执行按钮
                    with gr.Row():
                        clear_btn = gr.Button("🗑️ 清空", variant="secondary")
                        submit_btn = gr.Button("✨ 开始处理", variant="primary")

                with gr.Column(scale=1):
                    # 输出区域
                    output_image = gr.Image(label="处理结果", type="pil", height=500)
                    output_status = gr.Textbox(
                        label="状态信息",
                        lines=5,
                        interactive=False
                    )

            # 存储当前工作流的组件
            workflow_state = gr.State({
                'workflow_id': workflows[0]['id'],
                'input_components': [],
                'param_components': [],
                'preset_buttons': []
            })

            # 初始化第一个工作流的 UI
            initial_components = self._build_workflow_ui(
                workflows[0]['id'],
                inputs_column,
                params_column,
                presets_column
            )

            # 工作流切换事件
            def on_workflow_change(selected_name):
                # 找到对应的工作流 ID
                workflow_id = None
                selected_desc = ""

                for wf in workflows:
                    if f"{wf['icon']} {wf['name']}" == selected_name:
                        workflow_id = wf['id']
                        selected_desc = wf['description']
                        break

                if not workflow_id:
                    return "未找到工作流", {}

                # 构建新的 UI
                components = self._build_workflow_ui(
                    workflow_id,
                    inputs_column,
                    params_column,
                    presets_column
                )

                return f"**描述**: {selected_desc}", components

            workflow_dropdown.change(
                fn=on_workflow_change,
                inputs=[workflow_dropdown],
                outputs=[workflow_desc, workflow_state]
            )

            # 执行工作流
            def execute_workflow(state, *args):
                try:
                    workflow_id = state['workflow_id']
                    info = self.executor.get_workflow_info(workflow_id)

                    if not info:
                        return None, "❌ 工作流不存在"

                    # 解析输入
                    inputs = {}
                    input_count = len(info['inputs'])

                    for i, inp in enumerate(info['inputs']):
                        if i < len(args):
                            inputs[inp['name']] = args[i]

                    # 解析参数
                    params = {}
                    param_start = input_count

                    for i, param in enumerate(info['parameters']):
                        arg_index = param_start + i
                        if arg_index < len(args):
                            params[param['name']] = args[arg_index]

                    # 验证输入
                    for inp in info['inputs']:
                        if inp.get('required') and not inputs.get(inp['name']):
                            return None, f"❌ 缺少必需输入: {inp['label']}"

                    # 执行工作流
                    yield None, "⏳ 正在处理，请稍候..."

                    result = self.executor.execute(
                        workflow_id,
                        inputs,
                        params,
                        verbose=False
                    )

                    if result.get('success'):
                        # 获取第一个下载的图像
                        downloaded = result.get('downloaded_images', [])
                        if downloaded:
                            return downloaded[0]['image'], "✅ 处理完成！"
                        else:
                            return None, "⚠️ 处理完成，但未找到输出图像"
                    else:
                        return None, f"❌ 处理失败: {result.get('message', '未知错误')}"

                except Exception as e:
                    return None, f"❌ 错误: {str(e)}"

            # 绑定提交按钮
            submit_btn.click(
                fn=execute_workflow,
                inputs=[workflow_state] + initial_components['all_inputs'],
                outputs=[output_image, output_status]
            )

            # 清空按钮
            def clear_all():
                return None, ""

            clear_btn.click(
                fn=clear_all,
                inputs=[],
                outputs=[output_image, output_status]
            )

            # 刷新服务器状态
            def refresh_server_status():
                return self._get_server_status()

            refresh_btn.click(
                fn=refresh_server_status,
                inputs=[],
                outputs=[server_status]
            )

            # 使用提示
            with gr.Accordion("💡 使用说明", open=False):
                gr.Markdown("""
### 如何使用

1. **选择工作流**: 从下拉菜单中选择需要的 AI 处理任务
2. **上传输入**: 根据工作流要求上传图像或其他文件
3. **调整参数**: 使用滑块调整处理参数，或点击预设按钮快速应用
4. **开始处理**: 点击"开始处理"按钮执行工作流
5. **查看结果**: 处理完成后在右侧查看结果图像

### 添加新工作流

1. 将 ComfyUI 工作流导出为 API 格式的 JSON 文件
2. 在 `workflows/` 目录创建新文件夹
3. 添加 `workflow.json` 和 `schema.yaml` 配置
4. 在 `config/workflows.yaml` 注册工作流
5. 创建适配器类（继承 `BaseAdapter`）

详细说明请参考项目文档。
                """)

        return app

    def _build_workflow_ui(self, workflow_id: str, inputs_col, params_col, presets_col):
        """
        为指定工作流构建 UI 组件

        Args:
            workflow_id: 工作流 ID
            inputs_col: 输入列容器
            params_col: 参数列容器
            presets_col: 预设列容器

        Returns:
            组件字典
        """
        info = self.executor.get_workflow_info(workflow_id)

        if not info:
            return {'all_inputs': []}

        components = {
            'workflow_id': workflow_id,
            'inputs': [],
            'params': [],
            'presets': [],
            'all_inputs': []
        }

        # 清空现有组件
        with inputs_col:
            inputs_col.clear()
            gr.Markdown("### 📥 输入")

            for inp in info['inputs']:
                if inp['type'] == 'image':
                    comp = gr.Image(
                        label=inp['label'],
                        type="filepath",
                        info=inp.get('description', '')
                    )
                    components['inputs'].append(comp)
                    components['all_inputs'].append(comp)

        # 参数组件
        with params_col:
            params_col.clear()
            gr.Markdown("### ⚙️ 参数")

            # 按类别分组
            categories = {}
            for param in info['parameters']:
                cat = param.get('category', '基础参数')
                if cat not in categories:
                    categories[cat] = []
                categories[cat].append(param)

            # 为每个类别创建折叠面板
            for category, params in categories.items():
                with gr.Accordion(category, open=(category == '基础参数')):
                    for param in params:
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
                        elif param['type'] == 'select':
                            options = [opt['value'] for opt in param.get('options', [])]
                            comp = gr.Dropdown(
                                choices=options,
                                value=param['default'],
                                label=param['label'],
                                info=param.get('description', '')
                            )
                        else:
                            comp = gr.Textbox(
                                value=str(param.get('default', '')),
                                label=param['label'],
                                info=param.get('description', '')
                            )

                        components['params'].append(comp)
                        components['all_inputs'].append(comp)

        # 预设按钮
        with presets_col:
            presets_col.clear()

            if info['presets']:
                gr.Markdown("### 🎛️ 快速预设")

                with gr.Row():
                    for preset_id, preset_info in info['presets'].items():
                        btn = gr.Button(
                            f"{preset_info['icon']} {preset_info['name']}",
                            size="sm"
                        )
                        components['presets'].append(btn)

                        # TODO: 绑定预设按钮事件

        return components

    def _build_empty_interface(self) -> gr.Blocks:
        """构建空界面（无工作流时）"""
        with gr.Blocks(theme=gr.themes.Soft()) as app:
            gr.Markdown("# ⚠️ 未找到可用的工作流")
            gr.Markdown("""
请检查：
1. `config/workflows.yaml` 配置文件是否存在
2. 是否已注册并启用工作流
3. 工作流文件是否正确配置

参考文档了解如何添加工作流。
            """)

        return app

    def _get_server_status(self) -> str:
        """获取服务器状态文本"""
        if self.executor.check_server():
            return "✅ **ComfyUI 服务器**: 运行中"
        else:
            return f"❌ **ComfyUI 服务器**: 离线 ({self.executor.client.server_address})"

    def launch(self, server_port: int = None, share: bool = None):
        """
        启动 Web 应用

        Args:
            server_port: 服务器端口
            share: 是否创建公共链接
        """
        # 使用配置文件中的值或参数值
        port = server_port or self.config.get('server', {}).get('web_port', 7860)
        share_val = share if share is not None else self.config.get('server', {}).get('share', False)

        app = self.build_interface()

        print("\n" + "=" * 60)
        print("🚀 ComfyUI 多 API 适配器")
        print("=" * 60)
        print(f"📦 已加载工作流: {len(self.executor.workflow_manager)}")
        print(f"🌐 Web 界面端口: {port}")
        print(f"🔗 ComfyUI 服务器: {self.executor.client.server_address}")
        print("=" * 60 + "\n")

        app.launch(
            server_port=port,
            share=share_val,
            server_name="0.0.0.0"
        )


if __name__ == "__main__":
    app = MultiAPIApp()
    app.launch()

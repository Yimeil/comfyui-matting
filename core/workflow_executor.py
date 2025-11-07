"""
工作流执行器
"""

import json
import time
from typing import Dict, Any, Optional, List
from pathlib import Path

from .comfyui_client import ComfyUIClient
from .workflow_manager import WorkflowManager


class WorkflowExecutor:
    """工作流执行器 - 负责执行工作流"""

    def __init__(self, comfyui_url: str = "127.0.0.1:8188",
                 workflows_config: str = "config/workflows.yaml"):
        """
        初始化执行器

        Args:
            comfyui_url: ComfyUI 服务器地址
            workflows_config: 工作流配置文件路径
        """
        self.client = ComfyUIClient(comfyui_url)
        self.workflow_manager = WorkflowManager(workflows_config)

    def check_server(self) -> bool:
        """
        检查 ComfyUI 服务器是否可用

        Returns:
            服务器是否可用
        """
        return self.client.check_server_status()

    def execute(self, workflow_id: str, inputs: Dict[str, Any],
                params: Optional[Dict[str, Any]] = None,
                verbose: bool = True) -> Dict:
        """
        执行工作流

        Args:
            workflow_id: 工作流 ID
            inputs: 输入参数（如图像路径）
            params: 工作流参数
            verbose: 是否打印详细信息

        Returns:
            执行结果

        Raises:
            ValueError: 工作流不存在或参数验证失败
            Exception: 执行失败
        """
        # 获取适配器
        adapter = self.workflow_manager.get_adapter(workflow_id)
        if not adapter:
            raise ValueError(f"未找到工作流: {workflow_id}")

        # 验证输入
        if verbose:
            print(f"验证输入参数...")
        adapter.validate_inputs(inputs)

        # 加载工作流模板
        workflow_config = self.workflow_manager.get_workflow(workflow_id)
        workflow_file = workflow_config.get('workflow_file', '')

        if not workflow_file:
            raise ValueError(f"工作流配置错误: 未指定 workflow_file")

        if verbose:
            print(f"加载工作流: {workflow_file}")

        with open(workflow_file, 'r', encoding='utf-8') as f:
            workflow = json.load(f)

        # 上传输入文件
        uploaded_inputs = {}
        for input_name, input_value in inputs.items():
            input_info = self._get_input_info(adapter, input_name)

            if input_info and input_info.get('type') == 'image':
                # 如果是图像，上传到服务器
                if verbose:
                    print(f"上传 {input_name}: {input_value}")
                uploaded_filename = self.client.upload_image(input_value)
                uploaded_inputs[input_name] = uploaded_filename
            else:
                uploaded_inputs[input_name] = input_value

        # 准备参数
        params = params or {}

        # 验证参数
        if params:
            if verbose:
                print("验证参数...")
            adapter.validate_params(params)

        # 合并默认参数
        final_params = adapter.get_default_params()
        final_params.update(params)

        # 准备工作流
        if verbose:
            print("准备工作流...")
        workflow = adapter.prepare_workflow(workflow, uploaded_inputs, final_params)

        # 提交执行
        if verbose:
            print("提交工作流到 ComfyUI...")
        result = self.client.queue_prompt(workflow)
        prompt_id = result.get('prompt_id', '')

        if not prompt_id:
            raise Exception("未能获取 prompt_id")

        if verbose:
            print(f"Prompt ID: {prompt_id}")

        # 跟踪进度
        if verbose:
            print("跟踪执行进度...")
        self.client.track_progress(prompt_id, verbose=verbose)

        # 获取结果
        if verbose:
            print("获取执行结果...")
        history = self.client.get_history(prompt_id)

        if prompt_id not in history:
            raise Exception("未找到执行历史记录")

        outputs = history[prompt_id].get('outputs', {})

        # 处理输出
        if verbose:
            print("处理输出...")
        processed_result = adapter.process_outputs(outputs)

        # 下载输出图像
        if processed_result.get('success') and processed_result.get('images'):
            downloaded_images = []

            for img_info in processed_result['images']:
                filename = img_info.get('filename', '')
                subfolder = img_info.get('subfolder', '')
                folder_type = img_info.get('type', 'output')

                if filename:
                    if verbose:
                        print(f"下载图像: {filename}")
                    try:
                        image = self.client.get_image(filename, subfolder, folder_type)
                        downloaded_images.append({
                            'filename': filename,
                            'image': image,
                            'subfolder': subfolder
                        })
                    except Exception as e:
                        if verbose:
                            print(f"下载图像失败: {e}")

            processed_result['downloaded_images'] = downloaded_images

        return processed_result

    def execute_with_preset(self, workflow_id: str, inputs: Dict[str, Any],
                           preset_name: str, verbose: bool = True) -> Dict:
        """
        使用预设配置执行工作流

        Args:
            workflow_id: 工作流 ID
            inputs: 输入参数
            preset_name: 预设名称
            verbose: 是否打印详细信息

        Returns:
            执行结果
        """
        adapter = self.workflow_manager.get_adapter(workflow_id)
        if not adapter:
            raise ValueError(f"未找到工作流: {workflow_id}")

        # 获取预设参数
        preset_params = adapter.apply_preset(preset_name)

        if not preset_params:
            raise ValueError(f"未找到预设: {preset_name}")

        if verbose:
            print(f"应用预设: {preset_name}")
            print(f"预设参数: {preset_params}")

        return self.execute(workflow_id, inputs, preset_params, verbose)

    def list_workflows(self) -> List[Dict]:
        """
        列出所有可用的工作流

        Returns:
            工作流列表
        """
        return self.workflow_manager.list_workflows()

    def get_workflow_info(self, workflow_id: str) -> Optional[Dict]:
        """
        获取工作流详细信息

        Args:
            workflow_id: 工作流 ID

        Returns:
            工作流信息
        """
        return self.workflow_manager.get_workflow_info(workflow_id)

    def _get_input_info(self, adapter, input_name: str) -> Optional[Dict]:
        """
        获取输入定义

        Args:
            adapter: 适配器实例
            input_name: 输入名称

        Returns:
            输入信息
        """
        for inp in adapter.get_input_schema():
            if inp.get('name') == input_name:
                return inp
        return None

    def __repr__(self):
        return f"<WorkflowExecutor workflows={len(self.workflow_manager)}>"

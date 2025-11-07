"""
工作流管理器
"""

import yaml
import importlib
import os
from typing import Dict, List, Any, Optional
from pathlib import Path


class WorkflowManager:
    """工作流管理器 - 负责加载和管理所有工作流"""

    def __init__(self, config_path: str = "config/workflows.yaml"):
        """
        初始化工作流管理器

        Args:
            config_path: 工作流配置文件路径
        """
        self.config_path = config_path
        self.workflows = {}  # 工作流配置字典
        self.adapters = {}   # 适配器实例字典

        # 加载工作流配置
        self._load_workflows()

    def _load_workflows(self):
        """加载工作流配置并初始化适配器"""
        if not os.path.exists(self.config_path):
            print(f"警告: 配置文件不存在: {self.config_path}")
            return

        try:
            with open(self.config_path, 'r', encoding='utf-8') as f:
                config = yaml.safe_load(f)

            if not config or 'workflows' not in config:
                print("警告: 配置文件格式错误或为空")
                return

            # 遍历所有工作流配置
            for workflow_id, workflow_config in config['workflows'].items():
                # 检查是否启用
                if not workflow_config.get('enabled', True):
                    continue

                # 添加 workflow_id
                workflow_config['workflow_id'] = workflow_id

                # 保存工作流配置
                self.workflows[workflow_id] = workflow_config

                # 动态加载适配器
                try:
                    adapter_class = self._load_adapter_class(
                        workflow_config.get('adapter', '')
                    )
                    self.adapters[workflow_id] = adapter_class(workflow_config)
                    print(f"已加载工作流: {workflow_id} - {workflow_config.get('name', '')}")
                except Exception as e:
                    print(f"加载适配器失败 ({workflow_id}): {e}")

        except Exception as e:
            print(f"加载配置文件失败: {e}")

    def _load_adapter_class(self, adapter_path: str):
        """
        动态加载适配器类

        Args:
            adapter_path: 适配器类路径 (例如: "adapters.sam_matting_adapter.SAMMattingAdapter")

        Returns:
            适配器类
        """
        if not adapter_path:
            raise ValueError("适配器路径为空")

        # 分割模块路径和类名
        module_path, class_name = adapter_path.rsplit('.', 1)

        # 动态导入模块
        module = importlib.import_module(module_path)

        # 获取类
        adapter_class = getattr(module, class_name)

        return adapter_class

    def get_workflow(self, workflow_id: str) -> Optional[Dict]:
        """
        获取工作流配置

        Args:
            workflow_id: 工作流 ID

        Returns:
            工作流配置字典，未找到返回 None
        """
        return self.workflows.get(workflow_id)

    def get_adapter(self, workflow_id: str):
        """
        获取工作流适配器

        Args:
            workflow_id: 工作流 ID

        Returns:
            适配器实例，未找到返回 None
        """
        return self.adapters.get(workflow_id)

    def list_workflows(self) -> List[Dict]:
        """
        列出所有可用的工作流

        Returns:
            工作流信息列表
        """
        result = []

        for workflow_id, config in self.workflows.items():
            result.append({
                'id': workflow_id,
                'name': config.get('name', workflow_id),
                'description': config.get('description', ''),
                'icon': config.get('icon', '⚙️'),
                'category': config.get('category', '其他')
            })

        return result

    def get_workflow_schema(self, workflow_id: str) -> Dict:
        """
        获取工作流的参数定义

        Args:
            workflow_id: 工作流 ID

        Returns:
            参数定义字典
        """
        adapter = self.get_adapter(workflow_id)
        if adapter:
            return adapter.schema
        return {}

    def get_workflow_info(self, workflow_id: str) -> Optional[Dict]:
        """
        获取工作流的完整信息

        Args:
            workflow_id: 工作流 ID

        Returns:
            工作流完整信息字典
        """
        config = self.get_workflow(workflow_id)
        adapter = self.get_adapter(workflow_id)

        if not config or not adapter:
            return None

        return {
            'id': workflow_id,
            'name': config.get('name', ''),
            'description': config.get('description', ''),
            'icon': config.get('icon', '⚙️'),
            'category': config.get('category', '其他'),
            'inputs': adapter.get_input_schema(),
            'parameters': adapter.get_parameter_info(),
            'presets': adapter.get_presets(),
            'outputs': adapter.get_output_schema()
        }

    def reload(self):
        """重新加载工作流配置"""
        self.workflows.clear()
        self.adapters.clear()
        self._load_workflows()

    def __len__(self):
        """返回已加载的工作流数量"""
        return len(self.workflows)

    def __contains__(self, workflow_id: str):
        """检查工作流是否存在"""
        return workflow_id in self.workflows

    def __repr__(self):
        return f"<WorkflowManager workflows={len(self.workflows)}>"

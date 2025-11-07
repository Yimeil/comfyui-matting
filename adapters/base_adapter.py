"""
工作流适配器基类
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
import yaml
import os


class BaseAdapter(ABC):
    """工作流适配器基类"""

    def __init__(self, workflow_config: Dict[str, Any]):
        """
        初始化适配器

        Args:
            workflow_config: 工作流配置字典
        """
        self.workflow_id = workflow_config.get('workflow_id', '')
        self.name = workflow_config.get('name', '')
        self.description = workflow_config.get('description', '')
        self.workflow_file = workflow_config.get('workflow_file', '')
        self.schema_file = workflow_config.get('schema_file', '')
        self.icon = workflow_config.get('icon', '⚙️')
        self.category = workflow_config.get('category', '其他')

        # 加载参数定义
        self.schema = self._load_schema()

    def _load_schema(self) -> Dict[str, Any]:
        """
        加载工作流参数定义

        Returns:
            参数定义字典
        """
        if not os.path.exists(self.schema_file):
            print(f"警告: 未找到 schema 文件: {self.schema_file}")
            return {}

        try:
            with open(self.schema_file, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f) or {}
        except Exception as e:
            print(f"加载 schema 文件失败: {e}")
            return {}

    @abstractmethod
    def validate_inputs(self, inputs: Dict[str, Any]) -> bool:
        """
        验证输入参数

        Args:
            inputs: 输入参数字典

        Returns:
            验证是否通过

        Raises:
            ValueError: 验证失败时抛出
        """
        pass

    @abstractmethod
    def prepare_workflow(self, workflow: Dict, inputs: Dict, params: Dict) -> Dict:
        """
        准备工作流（更新参数和输入）

        Args:
            workflow: 工作流定义
            inputs: 输入文件
            params: 参数设置

        Returns:
            更新后的工作流
        """
        pass

    @abstractmethod
    def process_outputs(self, outputs: Dict) -> Dict:
        """
        处理输出结果

        Args:
            outputs: ComfyUI 返回的输出

        Returns:
            处理后的结果
        """
        pass

    def get_parameter_info(self) -> List[Dict]:
        """
        获取参数信息

        Returns:
            参数列表
        """
        return self.schema.get('parameters', [])

    def get_presets(self) -> Dict[str, Dict]:
        """
        获取预设配置

        Returns:
            预设字典
        """
        return self.schema.get('presets', {})

    def get_input_schema(self) -> List[Dict]:
        """
        获取输入定义

        Returns:
            输入列表
        """
        return self.schema.get('inputs', [])

    def get_output_schema(self) -> List[Dict]:
        """
        获取输出定义

        Returns:
            输出列表
        """
        return self.schema.get('outputs', [])

    def apply_preset(self, preset_name: str) -> Dict[str, Any]:
        """
        应用预设配置

        Args:
            preset_name: 预设名称

        Returns:
            预设参数字典
        """
        presets = self.get_presets()
        if preset_name in presets:
            return presets[preset_name].get('params', {})
        return {}

    def get_parameter_by_name(self, name: str) -> Optional[Dict]:
        """
        根据名称获取参数信息

        Args:
            name: 参数名称

        Returns:
            参数信息字典，未找到返回 None
        """
        for param in self.get_parameter_info():
            if param.get('name') == name:
                return param
        return None

    def get_default_params(self) -> Dict[str, Any]:
        """
        获取默认参数值

        Returns:
            默认参数字典
        """
        defaults = {}
        for param in self.get_parameter_info():
            if 'default' in param:
                defaults[param['name']] = param['default']
        return defaults

    def validate_params(self, params: Dict[str, Any]) -> bool:
        """
        验证参数值

        Args:
            params: 参数字典

        Returns:
            验证是否通过

        Raises:
            ValueError: 验证失败时抛出
        """
        for param_name, param_value in params.items():
            param_info = self.get_parameter_by_name(param_name)

            if not param_info:
                # 忽略未定义的参数
                continue

            param_type = param_info.get('type')

            # 类型检查
            if param_type == 'float' and not isinstance(param_value, (int, float)):
                raise ValueError(f"参数 {param_name} 应为数字类型")
            elif param_type == 'int' and not isinstance(param_value, int):
                raise ValueError(f"参数 {param_name} 应为整数类型")
            elif param_type == 'select' and param_value not in [opt['value'] for opt in param_info.get('options', [])]:
                raise ValueError(f"参数 {param_name} 的值无效")

            # 范围检查
            if 'min' in param_info and param_value < param_info['min']:
                raise ValueError(f"参数 {param_name} 不能小于 {param_info['min']}")
            if 'max' in param_info and param_value > param_info['max']:
                raise ValueError(f"参数 {param_name} 不能大于 {param_info['max']}")

        return True

    def __repr__(self):
        return f"<{self.__class__.__name__} id='{self.workflow_id}' name='{self.name}'>"

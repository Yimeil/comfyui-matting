"""
SAM 智能抠图适配器
"""

from typing import Dict, Any
from .base_adapter import BaseAdapter


class SAMMattingAdapter(BaseAdapter):
    """SAM 抠图适配器"""

    def validate_inputs(self, inputs: Dict[str, Any]) -> bool:
        """
        验证输入

        Args:
            inputs: 输入字典

        Returns:
            验证是否通过

        Raises:
            ValueError: 验证失败时抛出
        """
        # 检查必需的输入
        required_inputs = ['image', 'mask']

        for inp in required_inputs:
            if inp not in inputs or not inputs[inp]:
                raise ValueError(f"缺少必需输入: {inp}")

        return True

    def prepare_workflow(self, workflow: Dict, inputs: Dict, params: Dict) -> Dict:
        """
        准备工作流

        Args:
            workflow: 原始工作流
            inputs: 输入文件
            params: 参数设置

        Returns:
            更新后的工作流
        """
        # 更新图像输入（节点 2: LoadImage）
        if '2' in workflow:
            workflow['2']['inputs']['image'] = inputs.get('image', '')
            # 注意：ComfyUI 的 LoadImage 节点可能不直接支持 mask 参数
            # 如果 mask 是独立输入，可能需要另一个 LoadImage 节点
            # 这里假设工作流已配置好 mask 输入
            if 'mask' in inputs and inputs['mask']:
                # 如果工作流支持直接设置 mask
                workflow['2']['inputs']['mask'] = inputs.get('mask', '')

        # 更新参数
        for param_name, param_value in params.items():
            param_info = self.get_parameter_by_name(param_name)

            if param_info:
                node_id = str(param_info.get('node_id', ''))
                node_param = param_info.get('node_param', '')

                if node_id in workflow and node_param:
                    workflow[node_id]['inputs'][node_param] = param_value

        return workflow

    def process_outputs(self, outputs: Dict) -> Dict:
        """
        处理输出

        Args:
            outputs: ComfyUI 的输出结果

        Returns:
            处理后的结果
        """
        result = {
            'success': True,
            'images': [],
            'message': '抠图完成'
        }

        # 从 PreviewImage 节点（节点 22）获取结果
        if '22' in outputs:
            node_output = outputs['22']
            if 'images' in node_output:
                result['images'] = node_output['images']
        else:
            # 尝试其他可能的输出节点
            for node_id, node_output in outputs.items():
                if 'images' in node_output:
                    result['images'] = node_output['images']
                    break

        if not result['images']:
            result['success'] = False
            result['message'] = '未找到输出图像'

        return result

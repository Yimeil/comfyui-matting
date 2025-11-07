"""
ComfyUI 工作流 API 使用示例
演示如何通过 Python 调用 sam_mask_matting_api.json 工作流
"""

import json
import requests
import websocket
import uuid
from typing import Dict, Any
import io
from PIL import Image


class ComfyUIWorkflowClient:
    """ComfyUI 工作流 API 客户端"""

    def __init__(self, server_address: str = "127.0.0.1:8188"):
        """
        初始化客户端

        Args:
            server_address: ComfyUI 服务器地址，格式为 "host:port"
        """
        self.server_address = server_address
        self.client_id = str(uuid.uuid4())

    def load_workflow(self, workflow_path: str) -> Dict[str, Any]:
        """
        加载工作流 JSON 文件

        Args:
            workflow_path: 工作流 JSON 文件路径

        Returns:
            工作流配置字典
        """
        with open(workflow_path, 'r', encoding='utf-8') as f:
            return json.load(f)

    def upload_image(self, image_path: str) -> str:
        """
        上传图像到 ComfyUI 服务器

        Args:
            image_path: 本地图像文件路径

        Returns:
            服务器上的图像文件名
        """
        url = f"http://{self.server_address}/upload/image"

        with open(image_path, 'rb') as f:
            files = {'image': f}
            data = {'overwrite': 'true'}
            response = requests.post(url, files=files, data=data)

        if response.status_code == 200:
            return response.json()['name']
        else:
            raise Exception(f"上传图像失败: {response.text}")

    def update_workflow_inputs(self, workflow: Dict[str, Any],
                               image_filename: str,
                               mask_filename: str = None) -> Dict[str, Any]:
        """
        更新工作流中的输入参数

        Args:
            workflow: 工作流配置
            image_filename: 输入图像文件名
            mask_filename: 蒙版图像文件名（可选，默认使用相同图像）

        Returns:
            更新后的工作流配置
        """
        # 更新节点 2（加载图像）
        workflow["2"]["inputs"]["image"] = image_filename

        # 更新节点 3（加载蒙版）
        if mask_filename is None:
            mask_filename = image_filename
        workflow["3"]["inputs"]["image"] = mask_filename

        return workflow

    def queue_prompt(self, workflow: Dict[str, Any]) -> Dict[str, Any]:
        """
        将工作流加入执行队列

        Args:
            workflow: 工作流配置

        Returns:
            响应数据，包含 prompt_id
        """
        url = f"http://{self.server_address}/prompt"
        payload = {
            "prompt": workflow,
            "client_id": self.client_id
        }

        response = requests.post(url, json=payload)

        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"提交工作流失败: {response.text}")

    def get_image(self, filename: str, subfolder: str = "", folder_type: str = "output") -> Image.Image:
        """
        从服务器下载图像

        Args:
            filename: 图像文件名
            subfolder: 子文件夹
            folder_type: 文件夹类型（output/input/temp）

        Returns:
            PIL Image 对象
        """
        url = f"http://{self.server_address}/view"
        params = {
            "filename": filename,
            "subfolder": subfolder,
            "type": folder_type
        }

        response = requests.get(url, params=params)

        if response.status_code == 200:
            return Image.open(io.BytesIO(response.content))
        else:
            raise Exception(f"下载图像失败: {response.text}")

    def track_progress(self, prompt_id: str):
        """
        通过 WebSocket 跟踪执行进度

        Args:
            prompt_id: 提示 ID
        """
        ws_url = f"ws://{self.server_address}/ws?clientId={self.client_id}"
        ws = websocket.create_connection(ws_url)

        try:
            while True:
                message = ws.recv()
                if isinstance(message, str):
                    data = json.loads(message)

                    # 解析不同类型的消息
                    if data['type'] == 'executing':
                        node_id = data['data']['node']
                        if node_id is None:
                            print(f"工作流执行完成！")
                            break
                        else:
                            print(f"正在执行节点: {node_id}")

                    elif data['type'] == 'progress':
                        value = data['data']['value']
                        max_value = data['data']['max']
                        print(f"进度: {value}/{max_value}")

                    elif data['type'] == 'status':
                        print(f"状态更新: {data['data']}")

        finally:
            ws.close()

    def get_history(self, prompt_id: str) -> Dict[str, Any]:
        """
        获取执行历史

        Args:
            prompt_id: 提示 ID

        Returns:
            执行历史数据
        """
        url = f"http://{self.server_address}/history/{prompt_id}"
        response = requests.get(url)

        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"获取历史失败: {response.text}")


def example_usage():
    """使用示例"""

    # 1. 创建客户端
    client = ComfyUIWorkflowClient("127.0.0.1:8188")

    # 2. 加载工作流
    workflow = client.load_workflow("sam_mask_matting_api.json")

    # 3. 上传图像
    print("上传图像...")
    image_filename = client.upload_image("path/to/your/image.png")
    mask_filename = client.upload_image("path/to/your/mask.png")

    # 4. 更新工作流参数
    print("更新工作流参数...")
    workflow = client.update_workflow_inputs(workflow, image_filename, mask_filename)

    # 5. 可选：调整工作流参数
    # 例如：调整收缩量
    workflow["23"]["inputs"]["expand"] = -5  # 增加收缩量

    # 例如：调整模糊半径
    workflow["23"]["inputs"]["blur_radius"] = 2  # 增加模糊

    # 例如：调整形态学处理
    workflow["43"]["inputs"]["kernel_size"] = 8  # 增加核大小

    # 例如：调整 SAM 检测阈值
    workflow["10"]["inputs"]["mask_hint_threshold"] = 0.7  # 提高阈值

    # 6. 提交工作流
    print("提交工作流...")
    result = client.queue_prompt(workflow)
    prompt_id = result["prompt_id"]
    print(f"Prompt ID: {prompt_id}")

    # 7. 跟踪进度
    print("跟踪执行进度...")
    client.track_progress(prompt_id)

    # 8. 获取结果
    print("获取结果...")
    history = client.get_history(prompt_id)

    # 解析输出图像
    outputs = history[prompt_id]["outputs"]

    # 节点 22 (PreviewImage) 的输出
    if "22" in outputs:
        images_info = outputs["22"]["images"]
        for img_info in images_info:
            filename = img_info["filename"]
            subfolder = img_info.get("subfolder", "")

            # 下载并保存图像
            result_image = client.get_image(filename, subfolder)
            result_image.save("result_matting.png")
            print("结果已保存到: result_matting.png")


class WorkflowParameterPresets:
    """工作流参数预设"""

    @staticmethod
    def high_quality_preset(workflow: Dict[str, Any]) -> Dict[str, Any]:
        """
        高质量预设 - 适用于需要精细边缘的场景
        """
        # SAM 检测参数
        workflow["10"]["inputs"]["mask_hint_threshold"] = 0.75
        workflow["10"]["inputs"]["threshold"] = 1.2

        # 形态学处理
        workflow["43"]["inputs"]["kernel_size"] = 8

        # 蒙版精细化
        workflow["23"]["inputs"]["expand"] = -2  # 较少收缩
        workflow["23"]["inputs"]["blur_radius"] = 2  # 较大模糊

        return workflow

    @staticmethod
    def fast_preset(workflow: Dict[str, Any]) -> Dict[str, Any]:
        """
        快速预设 - 适用于需要快速处理的场景
        """
        # 使用较小的 SAM 模型（需要手动替换模型文件）
        # workflow["20"]["inputs"]["model_name"] = "sam_vit_b_01ec64.pth"

        # 减少处理
        workflow["43"]["inputs"]["kernel_size"] = 4
        workflow["23"]["inputs"]["expand"] = -3
        workflow["23"]["inputs"]["blur_radius"] = 1

        return workflow

    @staticmethod
    def soft_edge_preset(workflow: Dict[str, Any]) -> Dict[str, Any]:
        """
        柔和边缘预设 - 适用于需要自然过渡的场景
        """
        # 增加模糊和收缩
        workflow["23"]["inputs"]["expand"] = -5
        workflow["23"]["inputs"]["blur_radius"] = 3
        workflow["23"]["inputs"]["lerp_alpha"] = 0.8

        return workflow

    @staticmethod
    def sharp_edge_preset(workflow: Dict[str, Any]) -> Dict[str, Any]:
        """
        锐利边缘预设 - 适用于需要清晰边缘的场景
        """
        # 减少模糊，精确收缩
        workflow["23"]["inputs"]["expand"] = -1
        workflow["23"]["inputs"]["blur_radius"] = 0.5

        # 增强形态学处理
        workflow["43"]["inputs"]["kernel_size"] = 10

        return workflow


def advanced_example():
    """高级使用示例 - 批量处理和参数优化"""

    client = ComfyUIWorkflowClient()
    workflow_base = client.load_workflow("sam_mask_matting_api.json")

    # 批量处理多张图像
    image_paths = [
        "image1.png",
        "image2.png",
        "image3.png"
    ]

    for i, image_path in enumerate(image_paths):
        print(f"\n处理图像 {i+1}/{len(image_paths)}: {image_path}")

        # 复制工作流
        workflow = workflow_base.copy()

        # 上传并更新
        image_filename = client.upload_image(image_path)
        workflow = client.update_workflow_inputs(workflow, image_filename)

        # 应用高质量预设
        workflow = WorkflowParameterPresets.high_quality_preset(workflow)

        # 提交并等待
        result = client.queue_prompt(workflow)
        prompt_id = result["prompt_id"]

        client.track_progress(prompt_id)

        # 保存结果
        history = client.get_history(prompt_id)
        # ... 处理结果


if __name__ == "__main__":
    # 基础示例
    example_usage()

    # 高级示例
    # advanced_example()

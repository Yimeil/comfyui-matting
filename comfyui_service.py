"""
ComfyUI 服务 - 简化版
类似 word2picture 项目的简单架构
"""

import json
import os
import requests
import websocket
import uuid
import yaml
from typing import Dict, Any, Optional
from PIL import Image
import io


class ComfyUIService:
    """ComfyUI 服务 - 统一封装所有 ComfyUI 操作"""

    def __init__(self, config_path: str = "config.yaml"):
        """
        初始化服务

        Args:
            config_path: 配置文件路径
        """
        # 加载配置
        self.config = self._load_config(config_path)
        self.server_address = self.config.get('comfyui_api_url', '127.0.0.1:8188')
        self.workflows_dir = self.config.get('workflows_dir', 'workflows')
        self.timeout = self.config.get('timeout', 30)

        # 生成客户端 ID
        self.client_id = str(uuid.uuid4())

    def _load_config(self, config_path: str) -> Dict:
        """加载配置文件"""
        if not os.path.exists(config_path):
            print(f"警告: 配置文件不存在 {config_path}，使用默认配置")
            return {}

        with open(config_path, 'r', encoding='utf-8') as f:
            return yaml.safe_load(f) or {}

    def load_workflow(self, workflow_name: str) -> Dict[str, Any]:
        """
        从 workflows 目录加载工作流 JSON

        Args:
            workflow_name: 工作流文件名（如 "sam_matting.json"）

        Returns:
            工作流配置字典
        """
        workflow_path = os.path.join(self.workflows_dir, workflow_name)

        if not os.path.exists(workflow_path):
            raise FileNotFoundError(f"工作流文件不存在: {workflow_path}")

        with open(workflow_path, 'r', encoding='utf-8') as f:
            return json.load(f)

    def upload_image(self, image_path: str, overwrite: bool = True) -> str:
        """
        上传图片到 ComfyUI 服务器

        Args:
            image_path: 本地图片路径
            overwrite: 是否覆盖同名文件

        Returns:
            服务器上的图片文件名
        """
        url = f"http://{self.server_address}/upload/image"

        with open(image_path, 'rb') as f:
            files = {'image': f}
            data = {'overwrite': 'true' if overwrite else 'false'}
            response = requests.post(url, files=files, data=data, timeout=self.timeout)

        if response.status_code == 200:
            result = response.json()
            return result.get('name', '')
        else:
            raise Exception(f"上传图片失败: {response.text}")

    def update_workflow_params(self, workflow: Dict, node_id: str,
                              param_name: str, param_value: Any) -> Dict:
        """
        更新工作流中的参数

        Args:
            workflow: 工作流配置
            node_id: 节点 ID
            param_name: 参数名称
            param_value: 参数值

        Returns:
            更新后的工作流
        """
        if node_id in workflow and 'inputs' in workflow[node_id]:
            workflow[node_id]['inputs'][param_name] = param_value
        return workflow

    def execute_workflow(self, workflow: Dict, verbose: bool = True) -> Dict:
        """
        执行工作流

        Args:
            workflow: 工作流配置
            verbose: 是否显示详细信息

        Returns:
            执行结果
        """
        # 提交工作流
        url = f"http://{self.server_address}/prompt"
        payload = {
            "prompt": workflow,
            "client_id": self.client_id
        }

        response = requests.post(url, json=payload, timeout=self.timeout)

        if response.status_code != 200:
            raise Exception(f"提交工作流失败: {response.text}")

        result = response.json()
        prompt_id = result.get('prompt_id', '')

        if not prompt_id:
            raise Exception("未获取到 prompt_id")

        if verbose:
            print(f"✓ 工作流已提交 (ID: {prompt_id})")

        # 跟踪进度
        if verbose:
            self._track_progress(prompt_id)

        # 获取结果
        return self._get_result(prompt_id)

    def _track_progress(self, prompt_id: str):
        """通过 WebSocket 跟踪执行进度"""
        ws_url = f"ws://{self.server_address}/ws?clientId={self.client_id}"

        try:
            ws = websocket.create_connection(ws_url, timeout=self.timeout)

            while True:
                try:
                    message = ws.recv()
                    if isinstance(message, str):
                        data = json.loads(message)

                        if data.get('type') == 'executing':
                            node_id = data.get('data', {}).get('node')
                            if node_id is None:
                                print("✓ 执行完成")
                                break
                            else:
                                print(f"  执行节点: {node_id}")

                        elif data.get('type') == 'progress':
                            value = data.get('data', {}).get('value', 0)
                            max_value = data.get('data', {}).get('max', 0)
                            print(f"  进度: {value}/{max_value}")

                except websocket.WebSocketTimeoutException:
                    break
                except Exception:
                    break

            ws.close()

        except Exception as e:
            print(f"WebSocket 连接失败: {e}")

    def _get_result(self, prompt_id: str) -> Dict:
        """获取执行结果"""
        url = f"http://{self.server_address}/history/{prompt_id}"
        response = requests.get(url, timeout=self.timeout)

        if response.status_code != 200:
            raise Exception(f"获取结果失败: {response.text}")

        history = response.json()

        if prompt_id not in history:
            raise Exception("未找到执行历史")

        return history[prompt_id].get('outputs', {})

    def download_image(self, filename: str, output_path: str,
                      subfolder: str = "", folder_type: str = "output"):
        """
        从服务器下载图片并保存

        Args:
            filename: 服务器上的图片文件名
            output_path: 本地保存路径
            subfolder: 子文件夹
            folder_type: 文件夹类型
        """
        url = f"http://{self.server_address}/view"
        params = {
            "filename": filename,
            "subfolder": subfolder,
            "type": folder_type
        }

        response = requests.get(url, params=params, timeout=self.timeout)

        if response.status_code == 200:
            image = Image.open(io.BytesIO(response.content))
            image.save(output_path)
            print(f"✓ 已保存: {output_path}")
        else:
            raise Exception(f"下载图片失败: {response.text}")

    def check_server(self) -> bool:
        """检查服务器是否可用"""
        try:
            url = f"http://{self.server_address}/system_stats"
            response = requests.get(url, timeout=5)
            return response.status_code == 200
        except Exception:
            return False

    # 便捷方法：一键执行抠图
    def run_matting(self, workflow_name: str, input_image: str,
                   params: Optional[Dict] = None, output_dir: str = "output",
                   verbose: bool = True) -> str:
        """
        一键执行抠图工作流

        Args:
            workflow_name: 工作流文件名（如 "sam_matting.json"）
            input_image: 输入图片路径
            params: 参数字典，格式为 {节点ID: {参数名: 参数值}}
            output_dir: 输出目录
            verbose: 是否显示详细信息

        Returns:
            输出图片路径
        """
        if verbose:
            print(f"\n🚀 开始执行工作流: {workflow_name}")
            print(f"📁 输入图片: {input_image}")

        # 1. 加载工作流
        if verbose:
            print("\n1️⃣ 加载工作流...")
        workflow = self.load_workflow(workflow_name)

        # 2. 上传图片
        if verbose:
            print("\n2️⃣ 上传图片...")
        uploaded_name = self.upload_image(input_image)
        if verbose:
            print(f"✓ 图片已上传: {uploaded_name}")

        # 3. 更新工作流参数（假设节点 10 是图片加载节点）
        workflow = self.update_workflow_params(workflow, "10", "image", uploaded_name)

        # 4. 应用自定义参数
        if params:
            if verbose:
                print("\n3️⃣ 应用参数...")
            for node_id, node_params in params.items():
                for param_name, param_value in node_params.items():
                    workflow = self.update_workflow_params(
                        workflow, node_id, param_name, param_value
                    )
                    if verbose:
                        print(f"  节点 {node_id}.{param_name} = {param_value}")

        # 5. 执行工作流
        if verbose:
            print("\n4️⃣ 执行工作流...")
        outputs = self.execute_workflow(workflow, verbose=verbose)

        # 6. 下载结果
        if verbose:
            print("\n5️⃣ 下载结果...")

        os.makedirs(output_dir, exist_ok=True)

        # 查找输出图片（遍历所有节点输出）
        output_path = None
        for node_id, node_output in outputs.items():
            if 'images' in node_output:
                for img_info in node_output['images']:
                    filename = img_info.get('filename', '')
                    subfolder = img_info.get('subfolder', '')

                    if filename:
                        # 生成输出文件名
                        base_name = os.path.splitext(os.path.basename(input_image))[0]
                        ext = os.path.splitext(filename)[1]
                        output_path = os.path.join(output_dir, f"{base_name}_matting{ext}")

                        # 下载图片
                        self.download_image(filename, output_path, subfolder)

        if not output_path:
            raise Exception("未找到输出图片")

        if verbose:
            print(f"\n✅ 完成！结果已保存到: {output_path}")

        return output_path

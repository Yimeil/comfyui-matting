"""
ComfyUI API 客户端
"""

import json
import requests
import websocket
import uuid
import io
from typing import Dict, Any, Optional
from PIL import Image


class ComfyUIClient:
    """ComfyUI API 客户端 - 负责与 ComfyUI 服务器通信"""

    def __init__(self, server_address: str = "127.0.0.1:8188", timeout: int = 10):
        """
        初始化客户端

        Args:
            server_address: ComfyUI 服务器地址，格式为 "host:port"
            timeout: 连接超时时间（秒）
        """
        self.server_address = server_address
        self.client_id = str(uuid.uuid4())
        self.timeout = timeout

    def check_server_status(self) -> bool:
        """
        检查服务器是否可用

        Returns:
            服务器是否可用
        """
        try:
            url = f"http://{self.server_address}/system_stats"
            response = requests.get(url, timeout=self.timeout)
            return response.status_code == 200
        except Exception:
            return False

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

    def upload_image(self, image_path: str, overwrite: bool = True) -> str:
        """
        上传图像到 ComfyUI 服务器

        Args:
            image_path: 本地图像文件路径
            overwrite: 是否覆盖同名文件

        Returns:
            服务器上的图像文件名
        """
        url = f"http://{self.server_address}/upload/image"

        with open(image_path, 'rb') as f:
            files = {'image': f}
            data = {'overwrite': 'true' if overwrite else 'false'}
            response = requests.post(url, files=files, data=data, timeout=30)

        if response.status_code == 200:
            result = response.json()
            return result.get('name', '')
        else:
            raise Exception(f"上传图像失败: {response.text}")

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

        response = requests.post(url, json=payload, timeout=self.timeout)

        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"提交工作流失败: {response.text}")

    def get_image(self, filename: str, subfolder: str = "",
                  folder_type: str = "output") -> Image.Image:
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

        response = requests.get(url, params=params, timeout=30)

        if response.status_code == 200:
            return Image.open(io.BytesIO(response.content))
        else:
            raise Exception(f"下载图像失败: {response.text}")

    def track_progress(self, prompt_id: str, verbose: bool = True):
        """
        通过 WebSocket 跟踪执行进度

        Args:
            prompt_id: 提示 ID
            verbose: 是否打印详细进度
        """
        ws_url = f"ws://{self.server_address}/ws?clientId={self.client_id}"

        try:
            ws = websocket.create_connection(ws_url, timeout=self.timeout)
        except Exception as e:
            if verbose:
                print(f"WebSocket 连接失败: {e}")
            return

        try:
            while True:
                try:
                    message = ws.recv()
                    if isinstance(message, str):
                        data = json.loads(message)

                        # 解析不同类型的消息
                        if data.get('type') == 'executing':
                            node_id = data.get('data', {}).get('node')
                            if node_id is None:
                                if verbose:
                                    print("工作流执行完成！")
                                break
                            else:
                                if verbose:
                                    print(f"正在执行节点: {node_id}")

                        elif data.get('type') == 'progress':
                            value = data.get('data', {}).get('value', 0)
                            max_value = data.get('data', {}).get('max', 0)
                            if verbose:
                                print(f"进度: {value}/{max_value}")

                        elif data.get('type') == 'status':
                            if verbose:
                                print(f"状态更新: {data.get('data')}")

                except websocket.WebSocketTimeoutException:
                    if verbose:
                        print("WebSocket 超时")
                    break
                except Exception as e:
                    if verbose:
                        print(f"接收消息失败: {e}")
                    break

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
        response = requests.get(url, timeout=self.timeout)

        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"获取历史失败: {response.text}")

    def get_queue(self) -> Dict[str, Any]:
        """
        获取队列状态

        Returns:
            队列信息
        """
        url = f"http://{self.server_address}/queue"
        response = requests.get(url, timeout=self.timeout)

        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"获取队列失败: {response.text}")

    def interrupt(self):
        """中断当前执行"""
        url = f"http://{self.server_address}/interrupt"
        response = requests.post(url, timeout=self.timeout)

        if response.status_code != 200:
            raise Exception(f"中断执行失败: {response.text}")

    def get_system_stats(self) -> Dict[str, Any]:
        """
        获取系统状态

        Returns:
            系统统计信息
        """
        url = f"http://{self.server_address}/system_stats"
        response = requests.get(url, timeout=self.timeout)

        if response.status_code == 200:
            return response.json()
        else:
            raise Exception(f"获取系统状态失败: {response.text}")

    def __repr__(self):
        return f"<ComfyUIClient server={self.server_address}>"

"""
AI 图像抠图 Web 应用
基于 Gradio 和 ComfyUI SAM Matting 工作流
"""

import gradio as gr
import requests
import json
from PIL import Image
import io
import time
import os

class MattingApp:
    """图像抠图应用核心逻辑"""

    def __init__(self, comfyui_url="http://127.0.0.1:8188"):
        self.comfyui_url = comfyui_url
        self.client_id = "gradio-client"
        self.workflow_path = "sam_mask_matting_api.json"

    def check_comfyui_status(self):
        """检查 ComfyUI 是否运行"""
        try:
            response = requests.get(f"{self.comfyui_url}/system_stats", timeout=3)
            return response.status_code == 200
        except:
            return False

    def upload_image(self, image: Image.Image, filename="temp.png"):
        """上传图像到 ComfyUI"""
        # 保存为临时文件
        temp_path = f"/tmp/{filename}"
        image.save(temp_path)

        # 上传
        url = f"{self.comfyui_url}/upload/image"
        with open(temp_path, 'rb') as f:
            files = {'image': f}
            data = {'overwrite': 'true'}
            response = requests.post(url, files=files, data=data)

        # 删除临时文件
        if os.path.exists(temp_path):
            os.remove(temp_path)

        if response.status_code == 200:
            return response.json()['name']
        else:
            raise Exception(f"上传失败: {response.text}")

    def process_image(self, image, mask,
                     mask_threshold, kernel_size,
                     expand, blur_radius,
                     progress=gr.Progress()):
        """处理图像抠图"""

        # 检查输入
        if image is None:
            return None, "❌ 请上传原始图像"

        if mask is None:
            return None, "❌ 请上传蒙版图像"

        # 检查 ComfyUI 状态
        progress(0, desc="检查服务状态...")
        if not self.check_comfyui_status():
            return None, "❌ ComfyUI 未运行，请确保 ComfyUI 在 http://127.0.0.1:8188 运行"

        try:
            # 上传图像
            progress(0.2, desc="上传图像中...")
            image_filename = self.upload_image(image, "input_image.png")

            progress(0.3, desc="上传蒙版中...")
            mask_filename = self.upload_image(mask, "input_mask.png")

            # 加载工作流
            progress(0.4, desc="加载工作流...")
            if not os.path.exists(self.workflow_path):
                return None, f"❌ 工作流文件不存在: {self.workflow_path}"

            with open(self.workflow_path, 'r', encoding='utf-8') as f:
                workflow = json.load(f)

            # 更新参数
            workflow["2"]["inputs"]["image"] = image_filename

            workflow["10"]["inputs"]["mask_hint_threshold"] = mask_threshold
            workflow["43"]["inputs"]["kernel_size"] = int(kernel_size)
            workflow["23"]["inputs"]["expand"] = int(expand)
            workflow["23"]["inputs"]["blur_radius"] = blur_radius

            # 提交工作流
            progress(0.5, desc="提交处理任务...")
            url = f"{self.comfyui_url}/prompt"
            payload = {"prompt": workflow, "client_id": self.client_id}
            response = requests.post(url, json=payload)

            if response.status_code != 200:
                return None, f"❌ 提交失败: {response.text}"

            prompt_id = response.json()["prompt_id"]

            # 等待完成（轮询）
            max_attempts = 60  # 最多等待 60 秒
            attempt = 0

            while attempt < max_attempts:
                progress(0.5 + (attempt / max_attempts) * 0.4, desc=f"处理中... ({attempt}s)")

                # 检查历史记录
                history_url = f"{self.comfyui_url}/history/{prompt_id}"
                history_response = requests.get(history_url)

                if history_response.status_code == 200:
                    history = history_response.json()

                    if prompt_id in history:
                        # 检查是否有输出
                        if "outputs" in history[prompt_id] and "22" in history[prompt_id]["outputs"]:
                            progress(0.9, desc="下载结果...")

                            # 获取输出图像
                            output_info = history[prompt_id]["outputs"]["22"]["images"][0]
                            filename = output_info["filename"]
                            subfolder = output_info.get("subfolder", "")

                            # 下载结果图像
                            view_url = f"{self.comfyui_url}/view"
                            params = {
                                "filename": filename,
                                "subfolder": subfolder,
                                "type": "output"
                            }
                            result_response = requests.get(view_url, params=params)

                            if result_response.status_code == 200:
                                result_image = Image.open(io.BytesIO(result_response.content))
                                progress(1.0, desc="完成！")
                                return result_image, "✅ 处理成功！"
                            else:
                                return None, f"❌ 下载结果失败: {result_response.text}"

                time.sleep(1)
                attempt += 1

            return None, "❌ 处理超时（60秒），请稍后重试"

        except Exception as e:
            return None, f"❌ 错误: {str(e)}"


# 创建应用实例
app = MattingApp()

# 定义 Gradio 界面
with gr.Blocks(
    title="AI 图像抠图工具",
    theme=gr.themes.Soft(
        primary_hue="blue",
        secondary_hue="sky",
    ),
    css="""
    .gradio-container {
        max-width: 1400px !important;
    }
    .main-title {
        text-align: center;
        color: #2563eb;
        margin-bottom: 2rem;
    }
    """
) as demo:

    gr.Markdown("""
    <h1 class="main-title">🎨 AI 智能图像抠图</h1>

    <p style="text-align: center; color: #64748b; font-size: 1.1em;">
    基于 SAM 模型的高质量图像抠图工具 - 上传图像和蒙版，调整参数，即可获得专业的抠图效果
    </p>
    """)

    with gr.Row():
        # 左侧：输入和参数
        with gr.Column(scale=1):
            gr.Markdown("### 📤 输入图像")

            image_input = gr.Image(
                label="1️⃣ 原始图像",
                type="pil",
                height=300,
                sources=["upload", "clipboard"]
            )

            mask_input = gr.Image(
                label="2️⃣ 蒙版图像（白色区域为保留部分）",
                type="pil",
                height=300,
                sources=["upload", "clipboard"]
            )

            gr.Markdown("### ⚙️ 参数调整")

            with gr.Accordion("🎯 基础参数", open=True):
                mask_threshold = gr.Slider(
                    minimum=0.1, maximum=1.0, value=0.6, step=0.05,
                    label="检测阈值",
                    info="提高可增加精度，推荐 0.6-0.8"
                )

                blur_radius = gr.Slider(
                    minimum=0, maximum=5, value=1, step=0.5,
                    label="边缘模糊半径",
                    info="增加可获得柔和边缘，推荐 1-3"
                )

            with gr.Accordion("🔧 高级参数", open=False):
                kernel_size = gr.Slider(
                    minimum=2, maximum=15, value=6, step=1,
                    label="形态学核大小",
                    info="增加可填充更大的孔洞，推荐 6-10"
                )

                expand = gr.Slider(
                    minimum=-10, maximum=10, value=-3, step=1,
                    label="蒙版收缩/扩张",
                    info="负值收缩（避免白边），正值扩张，推荐 -3 到 -5"
                )

            gr.Markdown("### 🎛️ 快速预设")

            with gr.Row():
                preset_portrait = gr.Button("👤 人像模式", variant="secondary", size="sm")
                preset_product = gr.Button("📦 产品模式", variant="secondary", size="sm")
                preset_hair = gr.Button("💇 毛发模式", variant="secondary", size="sm")

            with gr.Row():
                clear_btn = gr.Button("🗑️ 清空", variant="secondary")
                submit_btn = gr.Button("✨ 开始抠图", variant="primary", size="lg")

        # 右侧：输出结果
        with gr.Column(scale=1):
            gr.Markdown("### 📥 输出结果")

            result_output = gr.Image(
                label="抠图结果",
                type="pil",
                height=600,
                show_download_button=True
            )

            status_output = gr.Textbox(
                label="状态信息",
                interactive=False,
                lines=2
            )

    # 底部：使用提示
    with gr.Accordion("💡 使用提示和常见问题", open=False):
        gr.Markdown("""
        ### 📋 使用步骤

        1. **上传图像**：上传原始图像和蒙版（白色区域为要保留的部分）
        2. **调整参数**：根据需要调整参数，或使用快速预设
        3. **开始处理**：点击"开始抠图"按钮
        4. **下载结果**：处理完成后，点击结果图像右下角的下载按钮

        ### 🔧 常见问题解决

        | 问题 | 原因 | 解决方案 |
        |-----|------|---------|
        | ⚪ **有白边** | 蒙版收缩不足 | 增加收缩量：expand 改为 -4 或 -5 |
        | 🔲 **边缘太硬** | 模糊不足 | 增加模糊半径：blur_radius 改为 2-3 |
        | 🕳️ **有小孔** | 形态学处理不足 | 增加核大小：kernel_size 改为 8-10 |
        | 🎯 **不够精确** | 检测阈值太低 | 提高检测阈值：mask_threshold 改为 0.7-0.8 |
        | ❌ **处理失败** | ComfyUI 未运行 | 确保 ComfyUI 在 http://127.0.0.1:8188 运行 |

        ### 🎨 预设参数说明

        | 预设 | 适用场景 | 参数配置 |
        |-----|---------|---------|
        | 👤 **人像模式** | 人物照片、肖像 | 阈值 0.7，收缩 -4，模糊 2.5 |
        | 📦 **产品模式** | 产品图、清晰边缘 | 核大小 10，收缩 -1，模糊 0.3 |
        | 💇 **毛发模式** | 保留毛发细节 | 收缩 -1，模糊 1.5 |

        ### 📊 参数详解

        - **检测阈值** (0.1-1.0)：SAM 模型的置信度阈值，越高越精确但可能遗漏细节
        - **形态学核大小** (2-15)：填充孔洞的能力，越大填充越多
        - **蒙版收缩/扩张** (-10 到 10)：负值收缩避免白边，正值扩张保留更多
        - **边缘模糊半径** (0-5)：边缘羽化程度，越大越柔和

        ### 🚀 性能提示

        - 处理时间取决于图像大小，通常 5-30 秒
        - 建议图像尺寸不超过 2048px
        - 首次使用需要加载 SAM 模型，会较慢

        ### ⚙️ 环境要求

        - ComfyUI 必须在 `http://127.0.0.1:8188` 运行
        - 需要安装以下 ComfyUI 节点：
          - ComfyUI-Impact-Pack
          - ComfyUI-SEGS
          - comfyui_controlnet_aux
          - Morphology
        - 需要下载 SAM 模型：`sam_vit_h_4b8939.pth`
        """)

    # 事件处理：提交处理
    submit_btn.click(
        fn=app.process_image,
        inputs=[
            image_input, mask_input,
            mask_threshold, kernel_size,
            expand, blur_radius
        ],
        outputs=[result_output, status_output]
    )

    # 事件处理：清空
    def clear_all():
        return [
            None,  # image_input
            None,  # mask_input
            0.6,   # mask_threshold
            6,     # kernel_size
            -3,    # expand
            1,     # blur_radius
            None,  # result_output
            ""     # status_output
        ]

    clear_btn.click(
        fn=clear_all,
        outputs=[
            image_input, mask_input,
            mask_threshold, kernel_size, expand, blur_radius,
            result_output, status_output
        ]
    )

    # 事件处理：预设 - 人像模式
    def apply_portrait_preset():
        return [0.7, 6, -4, 2.5, "✅ 已应用人像模式预设"]

    preset_portrait.click(
        fn=apply_portrait_preset,
        outputs=[mask_threshold, kernel_size, expand, blur_radius, status_output]
    )

    # 事件处理：预设 - 产品模式
    def apply_product_preset():
        return [0.6, 10, -1, 0.3, "✅ 已应用产品模式预设"]

    preset_product.click(
        fn=apply_product_preset,
        outputs=[mask_threshold, kernel_size, expand, blur_radius, status_output]
    )

    # 事件处理：预设 - 毛发模式
    def apply_hair_preset():
        return [0.6, 6, -1, 1.5, "✅ 已应用毛发模式预设"]

    preset_hair.click(
        fn=apply_hair_preset,
        outputs=[mask_threshold, kernel_size, expand, blur_radius, status_output]
    )

    # 底部信息
    gr.Markdown("""
    ---
    <p style="text-align: center; color: #94a3b8; font-size: 0.9em;">
    Powered by <a href="https://github.com/comfyanonymous/ComfyUI" target="_blank">ComfyUI</a> +
    <a href="https://github.com/facebookresearch/segment-anything" target="_blank">SAM</a> +
    <a href="https://www.gradio.app/" target="_blank">Gradio</a>
    </p>
    """)


# 启动应用
if __name__ == "__main__":
    print("\n" + "="*60)
    print("🎨 AI 图像抠图应用启动中...")
    print("="*60)
    print("\n⚙️  环境要求：")
    print("   - ComfyUI 必须运行在 http://127.0.0.1:8188")
    print("   - 需要安装相应的自定义节点和 SAM 模型")
    print("\n📖 完整文档：请查看 WEB_APPLICATION_GUIDE.md")
    print("\n" + "="*60 + "\n")

    demo.launch(
        server_name="0.0.0.0",     # 允许外部访问
        server_port=7860,           # 端口
        share=False,                # 设置为 True 可获得公网分享链接
        show_error=True,            # 显示详细错误信息
        show_api=False,             # 不显示 API 文档
        favicon_path=None
    )

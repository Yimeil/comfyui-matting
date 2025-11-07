# 创建 AI 图像抠图 Web 应用指南

本指南将帮助你将 ComfyUI SAM Matting 工作流转换为一个可供业务人员直接在浏览器中使用的 Web 应用。

## 📋 目录

1. [方案概览](#方案概览)
2. [方案一：使用 Gradio（推荐，最快）](#方案一使用-gradio推荐最快)
3. [方案二：部署到 RunningHub 等平台](#方案二部署到-runninghub-等平台)
4. [方案三：自建完整 Web 应用](#方案三自建完整-web-应用)
5. [功能设计建议](#功能设计建议)

---

## 方案概览

| 方案 | 难度 | 开发时间 | 适用场景 | 特点 |
|-----|------|---------|---------|------|
| Gradio | ⭐ | 1-2小时 | 快速原型、内部使用 | 代码少、部署简单 |
| RunningHub | ⭐⭐ | 2-4小时 | 公开分享、社区 | 托管服务、免费 |
| 自建 Web | ⭐⭐⭐⭐ | 1-2周 | 完全定制、商业产品 | 灵活度高 |

---

## 方案一：使用 Gradio（推荐，最快）

### 为什么选择 Gradio？

- ✅ 10分钟即可创建可用的 Web 界面
- ✅ 自动生成美观的 UI
- ✅ 支持文件上传、参数调节、结果预览
- ✅ 可本地运行或部署到 Hugging Face Spaces
- ✅ 自带用户友好的界面元素

### 实现步骤

#### 1. 安装依赖

```bash
pip install gradio requests pillow
```

#### 2. 创建 Gradio 应用

创建文件 `gradio_app.py`：

```python
import gradio as gr
import requests
import json
from PIL import Image
import io
import time

class MattingApp:
    def __init__(self, comfyui_url="http://127.0.0.1:8188"):
        self.comfyui_url = comfyui_url
        self.client_id = "gradio-client"

    def upload_image(self, image_path):
        """上传图像到 ComfyUI"""
        url = f"{self.comfyui_url}/upload/image"
        with open(image_path, 'rb') as f:
            files = {'image': f}
            data = {'overwrite': 'true'}
            response = requests.post(url, files=files, data=data)
        return response.json()['name']

    def process_image(self, image, mask,
                     mask_threshold, kernel_size,
                     expand, blur_radius):
        """处理图像抠图"""

        # 保存临时图像
        image.save("temp_image.png")
        mask.save("temp_mask.png")

        # 上传图像
        image_filename = self.upload_image("temp_image.png")
        mask_filename = self.upload_image("temp_mask.png")

        # 加载工作流
        with open("sam_mask_matting_api.json", 'r') as f:
            workflow = json.load(f)

        # 更新参数
        workflow["2"]["inputs"]["image"] = image_filename

        # 调整参数
        workflow["10"]["inputs"]["mask_hint_threshold"] = mask_threshold
        workflow["43"]["inputs"]["kernel_size"] = int(kernel_size)
        workflow["23"]["inputs"]["expand"] = int(expand)
        workflow["23"]["inputs"]["blur_radius"] = blur_radius

        # 提交工作流
        url = f"{self.comfyui_url}/prompt"
        payload = {"prompt": workflow, "client_id": self.client_id}
        response = requests.post(url, json=payload)
        prompt_id = response.json()["prompt_id"]

        # 等待完成
        time.sleep(5)  # 简单等待，实际应该使用 WebSocket

        # 获取结果
        history_url = f"{self.comfyui_url}/history/{prompt_id}"
        history = requests.get(history_url).json()

        if prompt_id in history and "22" in history[prompt_id]["outputs"]:
            output_info = history[prompt_id]["outputs"]["22"]["images"][0]
            filename = output_info["filename"]
            subfolder = output_info.get("subfolder", "")

            # 下载结果图像
            view_url = f"{self.comfyui_url}/view"
            params = {"filename": filename, "subfolder": subfolder, "type": "output"}
            result_response = requests.get(view_url, params=params)

            return Image.open(io.BytesIO(result_response.content))

        return None

# 创建应用实例
app = MattingApp()

# 定义 Gradio 界面
def matting_interface(image, mask, mask_threshold, kernel_size, expand, blur_radius):
    """Gradio 接口函数"""
    if image is None:
        return None, "请上传图像"

    if mask is None:
        return None, "请上传蒙版"

    try:
        result = app.process_image(
            image, mask,
            mask_threshold, kernel_size,
            expand, blur_radius
        )

        if result:
            return result, "✅ 处理成功！"
        else:
            return None, "❌ 处理失败，请检查 ComfyUI 是否运行"

    except Exception as e:
        return None, f"❌ 错误: {str(e)}"

# 创建 Gradio 界面
with gr.Blocks(title="AI 图像抠图工具", theme=gr.themes.Soft()) as demo:
    gr.Markdown("""
    # 🎨 AI 智能图像抠图

    基于 SAM 模型的高质量图像抠图工具。上传图像和蒙版，调整参数，即可获得专业的抠图效果。
    """)

    with gr.Row():
        with gr.Column(scale=1):
            gr.Markdown("### 📤 输入")

            image_input = gr.Image(
                label="原始图像",
                type="pil",
                height=300
            )

            mask_input = gr.Image(
                label="蒙版图像（白色区域为保留区域）",
                type="pil",
                height=300
            )

            gr.Markdown("### ⚙️ 参数调整")

            with gr.Accordion("基础参数", open=True):
                mask_threshold = gr.Slider(
                    minimum=0.1, maximum=1.0, value=0.6, step=0.05,
                    label="🎯 检测阈值",
                    info="提高可增加精度，推荐 0.6-0.8"
                )

                blur_radius = gr.Slider(
                    minimum=0, maximum=5, value=1, step=0.5,
                    label="🌫️ 边缘模糊半径",
                    info="增加可获得柔和边缘"
                )

            with gr.Accordion("高级参数", open=False):
                kernel_size = gr.Slider(
                    minimum=2, maximum=15, value=6, step=1,
                    label="🔧 形态学核大小",
                    info="增加可填充更大的孔洞"
                )

                expand = gr.Slider(
                    minimum=-10, maximum=10, value=-3, step=1,
                    label="↔️ 蒙版收缩量",
                    info="负值收缩（避免白边），正值扩张"
                )

            with gr.Row():
                clear_btn = gr.Button("🗑️ 清空", variant="secondary")
                submit_btn = gr.Button("✨ 开始抠图", variant="primary", size="lg")

        with gr.Column(scale=1):
            gr.Markdown("### 📥 输出结果")

            result_output = gr.Image(
                label="抠图结果",
                type="pil",
                height=600
            )

            status_output = gr.Textbox(
                label="状态",
                interactive=False
            )

            gr.Markdown("""
            ### 💡 使用提示

            **常见问题解决：**
            - **有白边**: 增加收缩量（expand 改为 -4 或 -5）
            - **边缘太硬**: 增加模糊半径（blur_radius 改为 2-3）
            - **有小孔**: 增加核大小（kernel_size 改为 8-10）
            - **不够精确**: 提高检测阈值（mask_threshold 改为 0.7-0.8）

            **预设参数：**
            - **人像抠图**: 阈值 0.7，收缩 -4，模糊 2.5
            - **产品图**: 核大小 10，收缩 -1，模糊 0.3
            - **毛发细节**: 收缩 -1，模糊 1.5
            """)

    # 预设按钮
    gr.Markdown("### 🎛️ 快速预设")
    with gr.Row():
        preset_portrait = gr.Button("👤 人像模式")
        preset_product = gr.Button("📦 产品模式")
        preset_hair = gr.Button("💇 毛发模式")

    # 事件处理
    submit_btn.click(
        fn=matting_interface,
        inputs=[image_input, mask_input, mask_threshold, kernel_size, expand, blur_radius],
        outputs=[result_output, status_output]
    )

    clear_btn.click(
        fn=lambda: [None, None, 0.6, 6, -3, 1, None, ""],
        outputs=[image_input, mask_input, mask_threshold, kernel_size, expand, blur_radius, result_output, status_output]
    )

    # 预设
    preset_portrait.click(
        fn=lambda: [0.7, 6, -4, 2.5],
        outputs=[mask_threshold, kernel_size, expand, blur_radius]
    )

    preset_product.click(
        fn=lambda: [0.6, 10, -1, 0.3],
        outputs=[mask_threshold, kernel_size, expand, blur_radius]
    )

    preset_hair.click(
        fn=lambda: [0.6, 6, -1, 1.5],
        outputs=[mask_threshold, kernel_size, expand, blur_radius]
    )

# 启动应用
if __name__ == "__main__":
    demo.launch(
        server_name="0.0.0.0",  # 允许外部访问
        server_port=7860,        # 端口
        share=False,             # 设置为 True 可获得公网链接
        show_error=True
    )
```

#### 3. 运行应用

```bash
# 确保 ComfyUI 正在运行（默认 http://127.0.0.1:8188）
python gradio_app.py
```

访问 `http://localhost:7860` 即可使用！

#### 4. 部署到 Hugging Face Spaces（可选）

```bash
# 安装 huggingface_hub
pip install huggingface_hub

# 创建新 Space 并推送
huggingface-cli login
huggingface-cli repo create your-matting-app --type space --space_sdk gradio

# 将代码推送到 Space
git clone https://huggingface.co/spaces/your-username/your-matting-app
cd your-matting-app
cp ../gradio_app.py app.py
cp ../sam_mask_matting_api.json .
git add .
git commit -m "Initial commit"
git push
```

---

## 方案二：部署到 RunningHub 等平台

### RunningHub 平台

RunningHub (https://www.runninghub.cn) 是一个 AI 应用分享平台。

#### 部署步骤

1. **准备 Gradio 应用**（使用上面的 `gradio_app.py`）

2. **注册 RunningHub 账号**
   - 访问 https://www.runninghub.cn
   - 注册/登录账号

3. **创建新应用**
   - 点击"创建应用"
   - 选择"Gradio"类型
   - 上传代码和配置文件

4. **配置应用**
   ```yaml
   # app.yaml
   name: AI图像抠图工具
   description: 基于SAM模型的智能图像抠图
   sdk: gradio
   sdk_version: 4.0.0
   python_version: 3.10
   app_file: gradio_app.py
   ```

5. **依赖配置**
   ```txt
   # requirements.txt
   gradio>=4.0.0
   requests>=2.28.0
   pillow>=9.0.0
   ```

### 其他平台选择

| 平台 | 优势 | 适用场景 |
|-----|------|---------|
| **Hugging Face Spaces** | 国际知名、免费、社区活跃 | 开源项目、技术分享 |
| **Streamlit Cloud** | 简单易用、集成 GitHub | 数据应用、可视化 |
| **Railway / Render** | 支持自定义后端 | 完整 Web 应用 |

---

## 方案三：自建完整 Web 应用

如果需要更高的定制化程度，可以构建完整的 Web 应用。

### 技术栈

**后端：**
- FastAPI / Flask（Python）
- Node.js + Express（JavaScript）

**前端：**
- React / Vue / Svelte
- TailwindCSS（样式）
- Ant Design / Material-UI（组件库）

### 架构设计

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│  浏览器前端  │────▶│  API 服务器   │────▶│  ComfyUI    │
│  (React)    │◀────│  (FastAPI)   │◀────│  (8188端口) │
└─────────────┘     └──────────────┘     └─────────────┘
```

### 快速实现示例

#### 后端 API（FastAPI）

创建 `backend/main.py`：

```python
from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
import requests
import json
from typing import Optional
import io

app = FastAPI(title="图像抠图 API")

# 允许跨域
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

COMFYUI_URL = "http://127.0.0.1:8188"

@app.post("/api/matting")
async def process_matting(
    image: UploadFile = File(...),
    mask: UploadFile = File(...),
    mask_threshold: float = 0.6,
    kernel_size: int = 6,
    expand: int = -3,
    blur_radius: float = 1.0
):
    """图像抠图 API"""

    # 上传图像到 ComfyUI
    files = {'image': (image.filename, await image.read())}
    response = requests.post(f"{COMFYUI_URL}/upload/image", files=files)
    image_filename = response.json()['name']

    files = {'image': (mask.filename, await mask.read())}
    response = requests.post(f"{COMFYUI_URL}/upload/image", files=files)
    mask_filename = response.json()['name']

    # 加载并更新工作流
    with open("sam_mask_matting_api.json", 'r') as f:
        workflow = json.load(f)

    workflow["2"]["inputs"]["image"] = image_filename
    workflow["10"]["inputs"]["mask_hint_threshold"] = mask_threshold
    workflow["43"]["inputs"]["kernel_size"] = kernel_size
    workflow["23"]["inputs"]["expand"] = expand
    workflow["23"]["inputs"]["blur_radius"] = blur_radius

    # 提交工作流
    payload = {"prompt": workflow, "client_id": "fastapi-client"}
    response = requests.post(f"{COMFYUI_URL}/prompt", json=payload)
    prompt_id = response.json()["prompt_id"]

    return {
        "status": "success",
        "prompt_id": prompt_id,
        "message": "处理已提交"
    }

@app.get("/api/result/{prompt_id}")
async def get_result(prompt_id: str):
    """获取处理结果"""

    # 获取历史记录
    response = requests.get(f"{COMFYUI_URL}/history/{prompt_id}")
    history = response.json()

    if prompt_id not in history:
        return {"status": "processing"}

    if "22" not in history[prompt_id]["outputs"]:
        return {"status": "failed"}

    # 获取输出图像信息
    output_info = history[prompt_id]["outputs"]["22"]["images"][0]
    filename = output_info["filename"]
    subfolder = output_info.get("subfolder", "")

    # 下载图像
    params = {"filename": filename, "subfolder": subfolder, "type": "output"}
    response = requests.get(f"{COMFYUI_URL}/view", params=params)

    return StreamingResponse(
        io.BytesIO(response.content),
        media_type="image/png",
        headers={"Content-Disposition": f"attachment; filename=result.png"}
    )

@app.get("/api/health")
async def health_check():
    """健康检查"""
    try:
        response = requests.get(f"{COMFYUI_URL}/system_stats", timeout=3)
        return {"status": "ok", "comfyui": "connected"}
    except:
        return {"status": "error", "comfyui": "disconnected"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

#### 前端界面（React）

创建 `frontend/src/App.jsx`：

```jsx
import React, { useState } from 'react';
import { Upload, Button, Slider, Card, message, Spin } from 'antd';
import { InboxOutlined } from '@ant-design/icons';

const { Dragger } = Upload;

function App() {
  const [image, setImage] = useState(null);
  const [mask, setMask] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const [params, setParams] = useState({
    maskThreshold: 0.6,
    kernelSize: 6,
    expand: -3,
    blurRadius: 1.0
  });

  const handleProcess = async () => {
    if (!image || !mask) {
      message.error('请先上传图像和蒙版');
      return;
    }

    setLoading(true);
    const formData = new FormData();
    formData.append('image', image);
    formData.append('mask', mask);
    formData.append('mask_threshold', params.maskThreshold);
    formData.append('kernel_size', params.kernelSize);
    formData.append('expand', params.expand);
    formData.append('blur_radius', params.blurRadius);

    try {
      // 提交处理请求
      const response = await fetch('http://localhost:8000/api/matting', {
        method: 'POST',
        body: formData
      });

      const data = await response.json();
      const promptId = data.prompt_id;

      // 轮询获取结果
      let attempts = 0;
      const maxAttempts = 30;

      const checkResult = async () => {
        const resultResponse = await fetch(
          `http://localhost:8000/api/result/${promptId}`
        );

        if (resultResponse.headers.get('content-type')?.includes('image')) {
          const blob = await resultResponse.blob();
          const url = URL.createObjectURL(blob);
          setResult(url);
          setLoading(false);
          message.success('处理完成！');
        } else {
          attempts++;
          if (attempts < maxAttempts) {
            setTimeout(checkResult, 1000);
          } else {
            setLoading(false);
            message.error('处理超时');
          }
        }
      };

      setTimeout(checkResult, 2000);

    } catch (error) {
      setLoading(false);
      message.error('处理失败: ' + error.message);
    }
  };

  return (
    <div style={{ padding: '40px', maxWidth: '1400px', margin: '0 auto' }}>
      <h1 style={{ textAlign: 'center', marginBottom: '40px' }}>
        🎨 AI 智能图像抠图
      </h1>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
        {/* 左侧：输入 */}
        <Card title="📤 输入">
          <Dragger
            beforeUpload={(file) => {
              setImage(file);
              return false;
            }}
            maxCount={1}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽上传原始图像</p>
          </Dragger>

          <Dragger
            beforeUpload={(file) => {
              setMask(file);
              return false;
            }}
            maxCount={1}
            style={{ marginTop: '20px' }}
          >
            <p className="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p className="ant-upload-text">点击或拖拽上传蒙版图像</p>
          </Dragger>

          <div style={{ marginTop: '30px' }}>
            <h3>⚙️ 参数调整</h3>

            <div style={{ marginBottom: '20px' }}>
              <label>检测阈值: {params.maskThreshold}</label>
              <Slider
                min={0.1}
                max={1.0}
                step={0.05}
                value={params.maskThreshold}
                onChange={(v) => setParams({...params, maskThreshold: v})}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label>边缘模糊: {params.blurRadius}</label>
              <Slider
                min={0}
                max={5}
                step={0.5}
                value={params.blurRadius}
                onChange={(v) => setParams({...params, blurRadius: v})}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label>核大小: {params.kernelSize}</label>
              <Slider
                min={2}
                max={15}
                step={1}
                value={params.kernelSize}
                onChange={(v) => setParams({...params, kernelSize: v})}
              />
            </div>

            <div style={{ marginBottom: '20px' }}>
              <label>收缩量: {params.expand}</label>
              <Slider
                min={-10}
                max={10}
                step={1}
                value={params.expand}
                onChange={(v) => setParams({...params, expand: v})}
              />
            </div>
          </div>

          <Button
            type="primary"
            size="large"
            block
            onClick={handleProcess}
            loading={loading}
          >
            ✨ 开始抠图
          </Button>
        </Card>

        {/* 右侧：输出 */}
        <Card title="📥 输出结果">
          {loading ? (
            <div style={{ textAlign: 'center', padding: '100px 0' }}>
              <Spin size="large" />
              <p style={{ marginTop: '20px' }}>正在处理中...</p>
            </div>
          ) : result ? (
            <img
              src={result}
              alt="Result"
              style={{ width: '100%', borderRadius: '8px' }}
            />
          ) : (
            <div style={{ textAlign: 'center', padding: '100px 0', color: '#999' }}>
              <p>处理结果将显示在这里</p>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

export default App;
```

#### 运行应用

```bash
# 后端
cd backend
pip install fastapi uvicorn python-multipart
uvicorn main:app --reload --port 8000

# 前端
cd frontend
npm install
npm run dev
```

---

## 功能设计建议

### 核心功能

1. **图像上传**
   - 支持拖拽上传
   - 预览上传的图像
   - 支持常见图像格式（PNG、JPG、WEBP）

2. **参数调节**
   - 滑块控制关键参数
   - 预设方案快速切换
   - 实时参数提示

3. **结果展示**
   - 对比视图（原图 vs 结果）
   - 缩放和平移
   - 下载结果

### 增强功能

1. **批量处理**
   - 上传多张图像
   - 队列管理
   - 批量下载

2. **历史记录**
   - 保存处理记录
   - 重新应用参数
   - 收藏常用配置

3. **高级功能**
   - 在线编辑蒙版
   - AI 自动生成蒙版
   - 背景替换

4. **用户体验**
   - 处理进度显示
   - 错误提示和建议
   - 移动端适配

### UI/UX 设计要点

```
┌─────────────────────────────────────────┐
│  🎨 AI 智能图像抠图                      │
├─────────────────┬───────────────────────┤
│  📤 上传区域     │  📥 结果预览           │
│                 │                       │
│  [拖拽上传图像]  │  [处理结果显示]        │
│  [拖拽上传蒙版]  │                       │
│                 │  [下载] [分享]         │
├─────────────────┤                       │
│  ⚙️ 参数面板    │                       │
│                 │                       │
│  检测阈值 ━━●━━ │                       │
│  边缘模糊 ━━●━━ │                       │
│  核大小   ━━●━━ │                       │
│  收缩量   ━━●━━ │                       │
│                 │                       │
│  [人像] [产品]   │                       │
│  [毛发] [自定义] │                       │
│                 │                       │
│  [开始处理] ✨   │                       │
└─────────────────┴───────────────────────┘
```

---

## 部署和运维

### Docker 部署

创建 `Dockerfile`：

```dockerfile
FROM python:3.10-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 7860

# 启动应用
CMD ["python", "gradio_app.py"]
```

创建 `docker-compose.yml`：

```yaml
version: '3.8'

services:
  matting-app:
    build: .
    ports:
      - "7860:7860"
    environment:
      - COMFYUI_URL=http://comfyui:8188
    depends_on:
      - comfyui

  comfyui:
    image: comfyui/comfyui:latest
    ports:
      - "8188:8188"
    volumes:
      - ./models:/app/models
```

运行：

```bash
docker-compose up -d
```

### 性能优化

1. **缓存策略**
   - Redis 缓存处理结果
   - CDN 加速静态资源

2. **负载均衡**
   - Nginx 反向代理
   - 多个 ComfyUI 实例

3. **异步处理**
   - Celery 任务队列
   - WebSocket 实时通知

---

## 总结

| 需求 | 推荐方案 |
|-----|---------|
| 快速原型/内部使用 | Gradio |
| 公开分享/零成本 | Gradio + Hugging Face Spaces |
| 商业产品/完全定制 | FastAPI + React |
| 企业内部/高性能 | 自建 + Docker + K8s |

**下一步行动：**

1. 先用 Gradio 快速搭建原型（1小时）
2. 测试用户反馈和需求
3. 根据需求选择是否升级到完整 Web 应用

有任何问题，欢迎随时询问！

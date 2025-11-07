# ComfyUI SAM Matting 工作流 API

基于 Segment Anything Model (SAM) 的智能图像抠图工作流

## 📋 项目概述

这是一个使用 ComfyUI 构建的图像抠图（Image Matting）工作流，结合了：
- **SAM (Segment Anything Model)**: Meta 开发的强大分割模型
- **图像形态学处理**: 优化蒙版质量
- **边缘羽化技术**: 创建自然的抠图效果

## 🎯 主要特性

- ✅ **智能分割**: 基于 SAM ViT-H 模型的零样本分割
- ✅ **蒙版优化**: 形态学闭运算填充孔洞、平滑边缘
- ✅ **边缘羽化**: 可调节的收缩和模糊处理
- ✅ **参数灵活**: 丰富的参数配置满足不同需求
- ✅ **API 友好**: 标准的 ComfyUI API 格式

## 📁 文件说明

```
.
├── sam_mask_matting_api.json   # 工作流配置文件（核心）
├── WORKFLOW_ANALYSIS.md        # 工作流深度分析文档
├── NODE_PARAMETERS_GUIDE.md    # 节点参数详细调整指南
├── workflow_api_example.py     # Python API 调用示例
└── README.md                   # 本文件
```

## 🚀 快速开始

### 1. 前置要求

- ComfyUI 已安装并运行
- 已安装以下自定义节点：
  - `ComfyUI-Impact-Pack` (SAMLoader, SAMDetectorSegmented)
  - `ComfyUI-SEGS` (MaskToSEGS)
  - `comfyui_controlnet_aux` (GrowMaskWithBlur)
  - `Morphology` 节点包

- 已下载 SAM 模型：
  - `sam_vit_h_4b8939.pth` (推荐，最高质量)
  - 或其他 SAM 模型变体 (vit_b, vit_l)

### 2. 基础使用

#### 方式 A: 在 ComfyUI UI 中使用

1. 打开 ComfyUI Web 界面
2. 点击 "Load" 按钮
3. 选择 `sam_mask_matting_api.json`
4. 上传图像和蒙版
5. 点击 "Queue Prompt" 执行

#### 方式 B: 通过 API 调用

```python
from workflow_api_example import ComfyUIWorkflowClient

# 创建客户端
client = ComfyUIWorkflowClient("127.0.0.1:8188")

# 加载工作流
workflow = client.load_workflow("sam_mask_matting_api.json")

# 上传图像
image_file = client.upload_image("your_image.png")
mask_file = client.upload_image("your_mask.png")

# 更新参数
workflow = client.update_workflow_inputs(workflow, image_file, mask_file)

# 提交执行
result = client.queue_prompt(workflow)
```

详细 API 使用请参考 `workflow_api_example.py`

## 🎨 工作流程

```
输入图像 + 蒙版提示
    ↓
SAM 智能分割
    ↓
形态学闭运算 (填充孔洞)
    ↓
收缩 + 模糊 (边缘羽化)
    ↓
应用到原图
    ↓
输出抠图结果
```

详细的数据流分析请参考 `WORKFLOW_ANALYSIS.md`

## ⚙️ 核心参数

### 关键参数速查

| 参数 | 位置 | 默认值 | 作用 | 调整建议 |
|-----|------|-------|------|---------|
| `mask_hint_threshold` | 节点 10 | 0.6 | SAM 检测阈值 | 提高精度: 0.7-0.8 |
| `kernel_size` | 节点 43 | 6 | 形态学核大小 | 填充大孔: 8-10 |
| `expand` | 节点 23 | -3 | 蒙版收缩量 | 避免白边: -4 或 -5 |
| `blur_radius` | 节点 23 | 1 | 边缘模糊 | 柔和边缘: 2-3 |

完整参数说明请参考 `NODE_PARAMETERS_GUIDE.md`

## 📖 使用场景与参数预设

### 人像抠图（柔和边缘）

```json
{
  "10": {"inputs": {"mask_hint_threshold": 0.7}},
  "23": {"inputs": {"expand": -4, "blur_radius": 2.5}}
}
```

### 产品图（锐利边缘）

```json
{
  "43": {"inputs": {"kernel_size": 10}},
  "23": {"inputs": {"expand": -1, "blur_radius": 0.3}}
}
```

### 毛发细节保留

```json
{
  "15": {"inputs": {"drop_size": 3}},
  "23": {"inputs": {"expand": -1, "blur_radius": 1.5, "lerp_alpha": 0.8}}
}
```

更多预设请参考 `workflow_api_example.py` 中的 `WorkflowParameterPresets` 类

## 🛠️ 常见问题

### Q: 结果有白边怎么办？

A: 增加收缩量，将节点 23 的 `expand` 改为 -4 或 -5

### Q: 边缘太硬怎么办？

A: 增加模糊半径，将节点 23 的 `blur_radius` 改为 2-3

### Q: 分割不准确怎么办？

A:
- 检查输入蒙版质量
- 提高节点 10 的 `mask_hint_threshold` 到 0.7-0.8
- 尝试 `detection_hint: "center-2"` 或 `"center-3"`

### Q: 蒙版有小孔怎么办？

A: 增加节点 43 的 `kernel_size` 到 8-10，使用 `operation: "close"`

### Q: 处理速度慢怎么办？

A:
- 使用较小的 SAM 模型 (sam_vit_b)
- 减小形态学 `kernel_size`
- 使用 `detection_hint: "center-1"`

## 📊 节点说明

| 节点 ID | 类型 | 功能 |
|--------|------|------|
| 2 | LoadImage | 加载原始图像和蒙版（输出0: 图像，输出1: 蒙版） |
| 20 | SAMLoader | 加载 SAM 模型 |
| 15 | MaskToSEGS | 蒙版转换为分割段 |
| 10 | SAMDetectorSegmented | SAM 智能分割 |
| 44 | MaskToImage | 蒙版转图像 |
| 43 | Morphology | 形态学处理（闭运算） |
| 45 | ImageToMask | 图像转蒙版 |
| 23 | GrowMaskWithBlur | 收缩+模糊处理 |
| 21 | ETN_ApplyMaskToImage | 应用蒙版到图像 |
| 22 | PreviewImage | 预览结果 |

详细的节点分析请参考 `WORKFLOW_ANALYSIS.md`

## 🔍 深入学习

1. **工作流架构与原理**: 阅读 `WORKFLOW_ANALYSIS.md`
2. **参数调优指南**: 阅读 `NODE_PARAMETERS_GUIDE.md`
3. **API 编程**: 参考 `workflow_api_example.py`

## 💡 优化建议

### 提高质量
- 使用高质量的输入蒙版
- 提高 SAM 检测阈值
- 增加形态学处理强度

### 提高速度
- 使用较小的 SAM 模型
- 减少形态学迭代
- 降低图像分辨率

### 批量处理
- 使用 `workflow_api_example.py` 中的批量处理示例
- 可并行处理多张图像

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可

本项目遵循 MIT 许可证

## 🔗 相关资源

- [ComfyUI](https://github.com/comfyanonymous/ComfyUI)
- [Segment Anything (SAM)](https://github.com/facebookresearch/segment-anything)
- [ComfyUI-Impact-Pack](https://github.com/ltdrdata/ComfyUI-Impact-Pack)

## 📝 更新日志

### v1.0.0 (2025-11-07)
- ✨ 初始版本
- ✅ 完整的工作流配置
- 📚 详细的文档和示例
- 🎯 参数预设和调优指南

---

**提示**: 如需技术支持，请参考文档或提交 Issue

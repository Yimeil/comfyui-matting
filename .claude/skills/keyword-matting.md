# Keyword-Based Image Matting Skill

基于关键字的智能抠图功能，使用 SAM (Segment Anything Model) 和 Grounding DINO 实现精确的物体分割。

## 功能说明

通过输入关键字（中文或英文）来描述需要抠出的物体，系统会自动：
1. 将关键字翻译成英文（如果需要）
2. 使用 Grounding DINO 定位物体
3. 使用 SAM 进行精确分割
4. 应用 VITMatte 进行边缘优化

## 使用示例

### 关键字示例
- "红色袜子" - 抠出红色的袜子
- "人脸" - 抠出人脸
- "汽车" - 抠出汽车
- "猫咪" - 抠出猫
- "狗" - 抠出狗

## 参数说明

### 必需参数
- `keyword`: 描述要抠出物体的关键字（中文或英文）

### 可选参数
- `translateFrom`: 翻译源语言，默认 "chinese"
- `samModel`: SAM 模型选择，默认 "sam_vit_h (2.56GB)"
- `dinoModel`: Grounding DINO 模型，默认 "GroundingDINO_SwinT_OGC (694MB)"
- `threshold`: 检测阈值 (0.1-1.0)，默认 0.3
- `detailMethod`: 细节处理方法，默认 "VITMatte(local)"
- `detailErode`: 细节腐蚀 (0-20)，默认 6
- `detailDilate`: 细节膨胀 (0-20)，默认 6
- `blackPoint`: 黑点阈值 (0-1)，默认 0.15
- `whitePoint`: 白点阈值 (0-1)，默认 0.99
- `maxMegapixels`: 最大分辨率，默认 2.0
- `device`: 运行设备，默认 "cuda"

## 工作流

使用 ComfyUI 工作流: `matting_keyword_api.json`

节点结构：
- 节点 1: LoadImage - 加载输入图片
- 节点 4: ArgosTranslateTextNode - 翻译关键字
- 节点 2: LayerMask: SegmentAnythingUltra V2 - SAM + Grounding DINO 分割
- 节点 6: SaveImage - 保存结果

## API 端点

```
POST /api/matting/keyword
```

## 返回结果

成功时返回：
- `outputFilename`: 输出文件名
- `outputUrl`: 输出文件 URL
- `promptId`: ComfyUI 执行 ID
- `executionTime`: 执行时间（毫秒）
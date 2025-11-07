#!/bin/bash

# AI 图像抠图 Web 应用启动脚本

echo "=========================================="
echo "🎨 AI 图像抠图 Web 应用"
echo "=========================================="
echo ""

# 检查 Python
if ! command -v python3 &> /dev/null; then
    echo "❌ 错误：未找到 Python3"
    echo "请先安装 Python 3.8 或更高版本"
    exit 1
fi

echo "✅ Python 版本："
python3 --version
echo ""

# 检查是否安装了依赖
echo "📦 检查依赖..."
if ! python3 -c "import gradio" &> /dev/null; then
    echo "⚠️  未安装 Gradio，正在安装依赖..."
    pip3 install -r requirements.txt
else
    echo "✅ 依赖已安装"
fi
echo ""

# 检查 ComfyUI 是否运行
echo "🔍 检查 ComfyUI 状态..."
if curl -s http://127.0.0.1:8188/system_stats > /dev/null 2>&1; then
    echo "✅ ComfyUI 正在运行"
else
    echo "❌ 警告：ComfyUI 未在 http://127.0.0.1:8188 运行"
    echo "   请先启动 ComfyUI，否则应用将无法正常工作"
    echo ""
    read -p "是否继续启动 Web 应用？(y/N): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "已取消启动"
        exit 1
    fi
fi
echo ""

# 检查工作流文件
if [ ! -f "sam_mask_matting_api.json" ]; then
    echo "❌ 错误：未找到工作流文件 sam_mask_matting_api.json"
    exit 1
fi
echo "✅ 工作流文件存在"
echo ""

# 启动应用
echo "=========================================="
echo "🚀 启动 Web 应用..."
echo "=========================================="
echo ""
echo "📍 访问地址："
echo "   本地: http://localhost:7860"
echo "   局域网: http://$(hostname -I | awk '{print $1}'):7860"
echo ""
echo "💡 提示："
echo "   - 按 Ctrl+C 停止应用"
echo "   - 完整文档请查看 WEB_APPLICATION_GUIDE.md"
echo ""
echo "=========================================="
echo ""

# 运行应用
python3 gradio_app.py

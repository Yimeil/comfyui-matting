#!/bin/bash

# ComfyUI Matting Web Application 启动脚本
# 适用于 Linux 和 macOS

echo "=========================================="
echo "ComfyUI 图像抠图 Web 应用启动脚本"
echo "=========================================="
echo ""

# 检查 Java 是否安装
if ! command -v java &> /dev/null
then
    echo "错误: 未检测到 Java 运行环境"
    echo "请安装 Java 17 或更高版本"
    echo "下载地址: https://adoptium.net/"
    exit 1
fi

# 显示 Java 版本
echo "检测到 Java 版本:"
java -version
echo ""

# 检查 Maven 是否安装
if ! command -v mvn &> /dev/null
then
    echo "警告: 未检测到 Maven"
    echo "如果需要重新构建项目，请安装 Maven"
    echo ""
fi

# 检查是否需要构建
JAR_FILE="target/matting-web-app-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "未找到 JAR 文件，开始构建项目..."
    echo ""

    if command -v mvn &> /dev/null
    then
        echo "执行: mvn clean package -DskipTests"
        mvn clean package -DskipTests

        if [ $? -ne 0 ]; then
            echo ""
            echo "错误: Maven 构建失败"
            exit 1
        fi
    else
        echo "错误: 需要 Maven 来构建项目"
        echo "请安装 Maven 或手动构建项目"
        exit 1
    fi
else
    echo "找到已构建的 JAR 文件: $JAR_FILE"
fi

echo ""
echo "=========================================="
echo "启动 Web 应用..."
echo "=========================================="
echo ""
echo "访问地址: http://localhost:8080"
echo "按 Ctrl+C 停止服务"
echo ""

# 启动应用
java -Xms512m -Xmx2g -jar "$JAR_FILE"

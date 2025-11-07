@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ==========================================
echo ComfyUI 图像抠图 Web 应用启动脚本
echo ==========================================
echo.

REM 检查 Java 是否安装
java -version > nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未检测到 Java 运行环境
    echo 请安装 Java 17 或更高版本
    echo 下载地址: https://adoptium.net/
    pause
    exit /b 1
)

echo 检测到 Java 版本:
java -version
echo.

REM 检查 Maven 是否安装
mvn -version > nul 2>&1
if %errorlevel% neq 0 (
    echo 警告: 未检测到 Maven
    echo 如果需要重新构建项目，请安装 Maven
    echo.
)

REM 检查是否需要构建
set JAR_FILE=target\matting-web-app-1.0.0.jar

if not exist "%JAR_FILE%" (
    echo 未找到 JAR 文件，开始构建项目...
    echo.

    mvn -version > nul 2>&1
    if %errorlevel% equ 0 (
        echo 执行: mvn clean package -DskipTests
        call mvn clean package -DskipTests

        if %errorlevel% neq 0 (
            echo.
            echo 错误: Maven 构建失败
            pause
            exit /b 1
        )
    ) else (
        echo 错误: 需要 Maven 来构建项目
        echo 请安装 Maven 或手动构建项目
        pause
        exit /b 1
    )
) else (
    echo 找到已构建的 JAR 文件: %JAR_FILE%
)

echo.
echo ==========================================
echo 启动 Web 应用...
echo ==========================================
echo.
echo 访问地址: http://localhost:8080
echo 按 Ctrl+C 停止服务
echo.

REM 启动应用
java -Xms512m -Xmx2g -jar "%JAR_FILE%"

pause

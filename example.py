"""
ComfyUI 抠图服务使用示例
类似 word2picture 项目的简单用法
"""

from comfyui_service import ComfyUIService


def main():
    # 1. 初始化服务（会自动读取 config.yaml）
    print("初始化 ComfyUI 服务...")
    service = ComfyUIService()

    # 2. 检查服务器状态
    if not service.check_server():
        print("❌ 无法连接到 ComfyUI 服务器，请检查配置")
        print(f"   服务器地址: {service.server_address}")
        return

    print(f"✓ 已连接到 ComfyUI 服务器: {service.server_address}\n")

    # ========== 示例 1: 最简单的用法 ==========
    print("=" * 60)
    print("示例 1: 一键执行抠图（使用默认参数）")
    print("=" * 60)

    try:
        result = service.run_matting(
            workflow_name="sam_matting.json",
            input_image="test_input.jpg",  # 你的输入图片
            output_dir="output"
        )
        print(f"\n✅ 成功！结果: {result}")
    except Exception as e:
        print(f"\n❌ 失败: {e}")

    # ========== 示例 2: 自定义参数 ==========
    print("\n" + "=" * 60)
    print("示例 2: 自定义参数执行")
    print("=" * 60)

    # 参数格式：{节点ID: {参数名: 参数值}}
    # 节点ID 可以通过查看工作流 JSON 文件获得
    custom_params = {
        "15": {  # SAM 模型节点
            "threshold": 0.5
        },
        "23": {  # Alpha Matting 节点
            "alpha_matting": "true",
            "alpha_matting_foreground_threshold": 240,
            "alpha_matting_background_threshold": 10,
            "alpha_matting_erode_size": 10
        }
    }

    try:
        result = service.run_matting(
            workflow_name="sam_matting.json",
            input_image="test_input.jpg",
            params=custom_params,
            output_dir="output",
            verbose=True
        )
        print(f"\n✅ 成功！结果: {result}")
    except Exception as e:
        print(f"\n❌ 失败: {e}")

    # ========== 示例 3: 更底层的用法（完全控制） ==========
    print("\n" + "=" * 60)
    print("示例 3: 底层 API 用法（完全控制）")
    print("=" * 60)

    try:
        # 3.1 加载工作流
        workflow = service.load_workflow("sam_matting.json")
        print("✓ 工作流已加载")

        # 3.2 上传图片
        uploaded_name = service.upload_image("test_input.jpg")
        print(f"✓ 图片已上传: {uploaded_name}")

        # 3.3 更新工作流参数
        workflow = service.update_workflow_params(workflow, "10", "image", uploaded_name)
        workflow = service.update_workflow_params(workflow, "15", "threshold", 0.5)
        print("✓ 参数已更新")

        # 3.4 执行工作流
        outputs = service.execute_workflow(workflow, verbose=True)
        print("✓ 工作流执行完成")

        # 3.5 下载结果
        for node_id, node_output in outputs.items():
            if 'images' in node_output:
                for img_info in node_output['images']:
                    filename = img_info.get('filename')
                    if filename:
                        service.download_image(
                            filename=filename,
                            output_path=f"output/{filename}",
                            subfolder=img_info.get('subfolder', '')
                        )

        print("\n✅ 完成！")

    except Exception as e:
        print(f"\n❌ 失败: {e}")


if __name__ == "__main__":
    main()

"""
简单功能测试
"""

from comfyui_service import ComfyUIService

def test_initialization():
    """测试服务初始化"""
    print("测试 1: 服务初始化...")
    try:
        service = ComfyUIService()
        print(f"  ✓ 服务地址: {service.server_address}")
        print(f"  ✓ 工作流目录: {service.workflows_dir}")
        print(f"  ✓ 超时设置: {service.timeout}s")
        return True
    except Exception as e:
        print(f"  ✗ 失败: {e}")
        return False

def test_load_workflow():
    """测试工作流加载"""
    print("\n测试 2: 加载工作流...")
    try:
        service = ComfyUIService()
        workflow = service.load_workflow("sam_matting.json")
        node_count = len(workflow)
        print(f"  ✓ 工作流已加载")
        print(f"  ✓ 节点数量: {node_count}")
        return True
    except Exception as e:
        print(f"  ✗ 失败: {e}")
        return False

def test_update_params():
    """测试参数更新"""
    print("\n测试 3: 参数更新...")
    try:
        service = ComfyUIService()
        workflow = service.load_workflow("sam_matting.json")

        # 更新参数
        workflow = service.update_workflow_params(workflow, "10", "image", "test.jpg")
        workflow = service.update_workflow_params(workflow, "15", "threshold", 0.5)

        print(f"  ✓ 参数更新成功")
        print(f"  ✓ 节点 10 image: {workflow.get('10', {}).get('inputs', {}).get('image')}")
        print(f"  ✓ 节点 15 threshold: {workflow.get('15', {}).get('inputs', {}).get('threshold')}")
        return True
    except Exception as e:
        print(f"  ✗ 失败: {e}")
        return False

def test_server_check():
    """测试服务器连接"""
    print("\n测试 4: 服务器连接检查...")
    try:
        service = ComfyUIService()
        is_online = service.check_server()

        if is_online:
            print(f"  ✓ ComfyUI 服务器在线")
        else:
            print(f"  ⚠ ComfyUI 服务器离线（这是正常的，如果你没有运行 ComfyUI）")

        return True
    except Exception as e:
        print(f"  ✗ 失败: {e}")
        return False

def main():
    print("=" * 60)
    print("ComfyUI Matting Service - 基础功能测试")
    print("=" * 60)

    results = []
    results.append(test_initialization())
    results.append(test_load_workflow())
    results.append(test_update_params())
    results.append(test_server_check())

    print("\n" + "=" * 60)
    passed = sum(results)
    total = len(results)

    if passed == total:
        print(f"✅ 所有测试通过 ({passed}/{total})")
    else:
        print(f"⚠ 部分测试通过 ({passed}/{total})")

    print("=" * 60)

if __name__ == "__main__":
    main()

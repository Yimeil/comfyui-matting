package com.yimeil.comfyui.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Qwen多角度编辑请求参数
 */
@Data
public class QwenEditRequest {

    /**
     * 上传的图片文件（必需）
     */
    private MultipartFile image;

    // ========== 编辑指令参数 ==========

    /**
     * 用户编辑指令（会与系统指令和场景上下文拼接）
     */
    private String editInstruction = "将图中所有物品按照其真实世界的正常尺寸比例，合理摆放在儿童书桌上";

    /**
     * 场景上下文描述（可选，如"儿童书桌"、"办公桌"等）
     */
    private String sceneContext = "";

    /**
     * 系统预设指令（可选，默认使用工作流中的对象保护协议）
     */
    private String systemInstruction = "";

    // ========== 图像处理参数 ==========

    /**
     * 目标尺寸（默认1024）
     */
    private Integer targetSize = 1024;

    /**
     * 目标VL尺寸（默认384）
     */
    private Integer targetVlSize = 384;

    /**
     * 上采样方法（默认lanczos）
     */
    private String upscaleMethod = "lanczos";

    /**
     * 裁剪方法（默认center）
     */
    private String cropMethod = "center";

    /**
     * 图像缩放宽度（默认1024）
     */
    private Integer resizeWidth = 1024;

    /**
     * 图像缩放高度（默认0，自动计算）
     */
    private Integer resizeHeight = 0;

    /**
     * 缩放插值方法（默认lanczos）
     */
    private String interpolation = "lanczos";

    // ========== 背景移除参数 ==========

    /**
     * BiRefNet模型版本
     */
    private String birefnetModel = "BiRefNet-HR-matting";

    /**
     * 蒙版模糊
     */
    private Integer maskBlur = 0;

    /**
     * 蒙版偏移
     */
    private Integer maskOffset = 0;

    /**
     * 是否反转输出
     */
    private Boolean invertOutput = false;

    /**
     * 是否细化前景
     */
    private Boolean refineForeground = false;

    /**
     * 背景类型（Alpha/Color）
     */
    private String background = "Alpha";

    /**
     * 背景颜色（如果background为Color）
     */
    private String backgroundColor = "#222222";

    // ========== 采样参数 ==========

    /**
     * 随机种子（-1为随机）
     */
    private Long seed = 947307071580783L;

    /**
     * 采样步数
     */
    private Integer steps = 3;

    /**
     * CFG引导强度
     */
    private Double cfg = 1.2;

    /**
     * 采样器名称
     */
    private String samplerName = "euler";

    /**
     * 调度器
     */
    private String scheduler = "beta";

    /**
     * 去噪强度
     */
    private Double denoise = 0.85;

    // ========== 模型参数 ==========

    /**
     * ModelSamplingAuraFlow的shift参数
     */
    private Integer shift = 3;

    /**
     * CFGNorm强度
     */
    private Double cfgNormStrength = 1.0;

    /**
     * 放大模型名称
     */
    private String upscaleModelName = "1xDeJPG_realplksr_otf.pth";

    /**
     * 重复Latent批次数量
     */
    private Integer repeatAmount = 1;
}

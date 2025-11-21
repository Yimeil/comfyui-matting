package com.yimeil.comfyui.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * BiRefNet批量抠图请求参数
 */
@Data
public class BatchMattingRequest {

    // ========== 输入配置 ==========

    /**
     * 输入模式
     * image: 单张图片上传
     * urls: HTTP地址列表
     * zip: 压缩文件上传
     */
    private String inputMode = "image";

    /**
     * 单张图片文件（image模式）
     */
    private MultipartFile imageFile;

    /**
     * HTTP图片地址列表（urls模式，换行分隔）
     */
    private String imageUrls;

    /**
     * 压缩文件（zip模式）
     */
    private MultipartFile zipFile;

    // ========== 模型配置 ==========

    /**
     * BiRefNet模型版本
     * 可选: BiRefNet-DIS5K, BiRefNet-DIS5K_fp16, BiRefNet-general, BiRefNet-general-lite 等
     */
    private String modelVersion = "BiRefNet-DIS5K";

    /**
     * 设备选择
     * 可选: auto, cuda, cpu
     */
    private String device = "auto";

    /**
     * 是否使用精细化
     */
    private Boolean useRefine = true;

    // ========== 图像处理配置 ==========

    /**
     * 处理高度（保持比例缩放）
     */
    private Integer processHeight = 1024;

    /**
     * 背景颜色
     * 可选: transparency, white, black, green, blue, red
     */
    private String backgroundColor = "transparency";

    // ========== 蒙版处理配置 ==========

    /**
     * 蒙版扩展值（负值收缩）
     */
    private Integer maskExpand = 5;

    /**
     * 模糊半径
     */
    private Double blurRadius = 1.5;

    /**
     * 锥形边角
     */
    private Boolean taperedCorners = true;

    /**
     * 填充孔洞
     */
    private Boolean fillHoles = false;

    /**
     * Lerp Alpha值
     */
    private Double lerpAlpha = 1.0;

    /**
     * 衰减因子
     */
    private Double decayFactor = 1.0;

    // ========== 放大配置 ==========

    /**
     * 放大模型名称
     */
    private String upscaleModel = "4x-UltraSharp.pth";

    // ========== 输出配置 ==========

    /**
     * 输出文件名前缀
     */
    private String filenamePrefix = "matting/img";
}
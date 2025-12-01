package com.yimeil.comfyui.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Qwen图像翻译请求参数
 */
@Data
public class QwenTranslateRequest {

    /**
     * 上传的图片文件（必需）
     */
    private MultipartFile image;

    // ========== 翻译配置参数 ==========

    /**
     * 源语言（如：zh, en, ja, ko等）
     */
    private String sourceLang = "zh";

    /**
     * 目标语言（如：en, zh, ja, ko等）
     */
    private String targetLang = "en";

    /**
     * Qwen API密钥（从配置文件读取）
     */
    private String apiKey;

    /**
     * 上传方法（dashscope/oss）
     */
    private String uploadMethod = "dashscope";

    /**
     * 上传令牌（如果uploadMethod为oss）
     */
    private String uploadToken = "";

    /**
     * OSS终端节点（如果uploadMethod为oss）
     */
    private String ossEndpoint = "";

    /**
     * OSS存储桶（如果uploadMethod为oss）
     */
    private String ossBucket = "";

    // ========== 可选参数 ==========

    /**
     * 域名提示（可选）
     */
    private String domainHint = "";

    /**
     * 最大等待时间（秒）
     */
    private Integer maxWaitTime = 300;

    /**
     * 是否跳过图像分割
     */
    private Boolean skipImgSegment = false;

    // ========== 图像输入方式 ==========

    /**
     * 图像URL（如果不上传文件，可以直接提供URL）
     */
    private String imageUrl = "";
}

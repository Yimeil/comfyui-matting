package com.yimeil.comfyui.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 抠图请求参数
 */
@Data
public class MattingRequest {

    /**
     * 上传的图片文件
     */
    private MultipartFile image;

    /**
     * 工作流文件名（可选，默认使用配置文件中的默认工作流）
     */
    private String workflowName;

    // ========== SAM 检测参数 ==========

    /**
     * SAM 阈值
     */
    private Double threshold = 1.0;

    /**
     * 蒙版提示阈值
     */
    private Double maskHintThreshold = 0.6;

    /**
     * 膨胀
     */
    private Integer dilation = 0;

    /**
     * 边界框扩展
     */
    private Integer bboxExpansion = 1;

    // ========== 蒙版处理参数 ==========

    /**
     * 扩展
     */
    private Integer expand = -3;

    /**
     * 模糊半径
     */
    private Integer blurRadius = 1;

    // ========== Alpha Matting 参数（旧版兼容） ==========

    /**
     * Alpha Matting 是否启用
     */
    private Boolean alphaMatting = true;

    /**
     * Alpha Matting 前景阈值
     */
    private Integer alphaMattingForegroundThreshold = 240;

    /**
     * Alpha Matting 背景阈值
     */
    private Integer alphaMattingBackgroundThreshold = 10;

    /**
     * Alpha Matting 腐蚀大小
     */
    private Integer alphaMattingErodeSize = 10;

    /**
     * 关键字（用于关键字抠图）
     */
    private String keyword;
}

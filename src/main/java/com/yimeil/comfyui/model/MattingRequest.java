package com.yimeil.comfyui.model;

import lombok.Data;

/**
 * 抠图请求参数
 */
@Data
public class MattingRequest {

    /**
     * 工作流文件名（可选，默认使用配置文件中的默认工作流）
     */
    private String workflowName;

    /**
     * SAM 阈值（0.0-1.0）
     */
    private Double threshold = 0.3;

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
}

package com.yimeil.comfyui.model;

import lombok.Data;

/**
 * 抠图结果
 */
@Data
public class MattingResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 输出图片文件名
     */
    private String outputFilename;

    /**
     * 输出图片 URL (ComfyUI 远程)
     */
    private String remoteUrl;

    /**
     * Prompt ID
     */
    private String promptId;

    /**
     * 执行时间（毫秒）
     */
    private long executionTime;

    /**
     * 错误消息
     */
    private String errorMessage;
}

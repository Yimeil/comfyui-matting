package com.yimeil.comfyui.model;

import lombok.Data;
import java.util.List;

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
     * 输出图片文件名（单图片场景，兼容旧版）
     */
    private String outputFilename;

    /**
     * 输出图片 URL (ComfyUI 远程)（单图片场景，兼容旧版）
     */
    private String remoteUrl;

    /**
     * 多张输出图片（批次生成场景）
     */
    private List<ImageOutput> images;

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

    /**
     * 图片输出信息
     */
    @Data
    public static class ImageOutput {
        /**
         * 文件名
         */
        private String filename;

        /**
         * 远程URL
         */
        private String remoteUrl;
    }
}

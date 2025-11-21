package com.yimeil.comfyui.model;

import lombok.Data;

import java.util.List;

/**
 * BiRefNet批量抠图结果
 */
@Data
public class BatchMattingResult {
    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * Prompt ID
     */
    private String promptId;

    /**
     * 执行时间（毫秒）
     */
    private long executionTime;

    /**
     * 生成的图片数量
     */
    private int imageCount;

    /**
     * 输出图片列表
     */
    private List<ImageInfo> images;

    /**
     * 图片信息
     */
    @Data
    public static class ImageInfo {
        private String filename;
        private String remoteUrl;
        private String subfolder;
    }
}

package com.comfyui.matting.model;

import lombok.Data;

/**
 * 图像抠图请求参数
 */
@Data
public class MattingRequest {
    /** SAM 检测阈值 (0.0-1.0) */
    private Double maskHintThreshold = 0.6;

    /** 形态学核大小 */
    private Integer kernelSize = 6;

    /** 蒙版收缩量（负数表示收缩） */
    private Integer expand = -3;

    /** 边缘模糊半径 */
    private Double blurRadius = 1.0;

    /** 预设模式：default, portrait, product, hair */
    private String preset = "default";
}

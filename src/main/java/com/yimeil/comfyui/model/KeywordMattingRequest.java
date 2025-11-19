package com.yimeil.comfyui.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 关键字抠图请求参数
 */
@Data
public class KeywordMattingRequest {

    /**
     * 上传的图片文件
     */
    private MultipartFile image;

    /**
     * 关键字
     */
    private String keyword;

    /**
     * 翻译来源语言
     */
    private String translateFrom = "chinese";

    /**
     * SAM 模型
     */
    private String samModel = "sam_vit_h (2.56GB)";

    /**
     * DINO 模型
     */
    private String dinoModel = "GroundingDINO_SwinT_OGC (694MB)";

    /**
     * 阈值
     */
    private Double threshold = 0.3;

    /**
     * 细节处理方法
     */
    private String detailMethod = "VITMatte(local)";

    /**
     * 细节腐蚀大小
     */
    private Integer detailErode = 6;

    /**
     * 细节膨胀大小
     */
    private Integer detailDilate = 6;

    /**
     * 黑点阈值
     */
    private Double blackPoint = 0.15;

    /**
     * 白点阈值
     */
    private Double whitePoint = 0.99;

    /**
     * 最大百万像素
     */
    private Double maxMegapixels = 2.0;

    /**
     * 设备
     */
    private String device = "cuda";
}
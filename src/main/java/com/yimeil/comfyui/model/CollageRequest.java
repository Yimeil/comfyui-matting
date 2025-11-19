package com.yimeil.comfyui.model;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel产品拼接请求参数
 */
@Data
public class CollageRequest {

    // ========== Excel配置 ==========

    /**
     * Excel文件（可选，与excelUrl二选一）
     */
    private MultipartFile excelFile;

    /**
     * Excel文件URL（可选，与excelFile二选一）
     */
    private String excelUrl;

    /**
     * Sheet名称
     */
    private String sheetName = "Sheet1";

    /**
     * 组合SKU列
     */
    private String combinedSkuCol = "A";

    /**
     * SKU列
     */
    private String skuCol = "B";

    /**
     * 数量列
     */
    private String pcsCol = "C";

    /**
     * 图片URL列
     */
    private String urlCol = "D";

    /**
     * 起始行
     */
    private Integer startRow = 2;

    /**
     * 筛选组合SKU
     */
    private String filterCombinedSku = "";

    // ========== 拼接参数 ==========

    /**
     * 每张拼接图的图片数量
     */
    private Integer imagesPerCollage = 9;

    /**
     * 布局方式
     */
    private String layout = "auto";

    /**
     * 输出宽度
     */
    private Integer outputWidth = 1600;

    /**
     * 输出高度
     */
    private Integer outputHeight = 1600;

    /**
     * 间距
     */
    private Integer spacing = 0;

    /**
     * 最小间距
     */
    private Integer minSpacing = 10;

    /**
     * 外边距
     */
    private Integer outerPadding = 50;

    /**
     * 产品缩放比例
     */
    private Integer productScale = 80;

    /**
     * 裁剪边距
     */
    private Integer cropMargin = 1;

    /**
     * 跳过空白
     */
    private Boolean skipEmpty = false;

    // ========== 标签设置 ==========

    /**
     * 标签格式
     */
    private String labelFormat = "×{pcs}";

    /**
     * 标签字体大小
     */
    private Integer labelFontSize = 80;

    /**
     * 标签位置
     */
    private String labelPosition = "bottom";

    /**
     * 标签边距
     */
    private Integer labelMargin = 30;

    /**
     * 隐藏数量为1的标签
     */
    private Boolean hidePcsOne = true;

    // ========== 其他设置 ==========

    /**
     * 使用缓存
     */
    private Boolean useCache = true;

    /**
     * 缓存大小
     */
    private Integer cacheSize = 100;

    /**
     * 输出模式
     */
    private String outputMode = "by_combined_sku";

    /**
     * 文件名前缀
     */
    private String filenamePrefix = "collage/%date:yyyy-MM-dd%/";

    /**
     * 自适应方向
     */
    private String adaptiveDirection = "auto";
}

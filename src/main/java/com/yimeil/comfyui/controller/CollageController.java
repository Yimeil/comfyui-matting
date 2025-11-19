package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.CollageResult;
import com.yimeil.comfyui.service.ComfyUIService;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Excel产品拼接 API 控制器
 */
@RestController
@RequestMapping("/api/collage")
@Slf4j
public class CollageController {

    @Autowired
    private ComfyUIService comfyUIService;

    /**
     * 执行Excel产品拼接
     */
    @PostMapping("/execute")
    public ApiResponse<CollageResult> executeCollage(
            @RequestParam(value = "excelFile", required = false) MultipartFile excelFile,
            @RequestParam(value = "excelUrl", required = false) String excelUrl,
            // Excel配置
            @RequestParam(value = "sheetName", defaultValue = "Sheet1") String sheetName,
            @RequestParam(value = "combinedSkuCol", defaultValue = "A") String combinedSkuCol,
            @RequestParam(value = "skuCol", defaultValue = "B") String skuCol,
            @RequestParam(value = "pcsCol", defaultValue = "C") String pcsCol,
            @RequestParam(value = "urlCol", defaultValue = "D") String urlCol,
            @RequestParam(value = "startRow", defaultValue = "2") Integer startRow,
            @RequestParam(value = "filterCombinedSku", defaultValue = "") String filterCombinedSku,
            // 拼接参数
            @RequestParam(value = "imagesPerCollage", defaultValue = "9") Integer imagesPerCollage,
            @RequestParam(value = "layout", defaultValue = "auto") String layout,
            @RequestParam(value = "outputWidth", defaultValue = "1600") Integer outputWidth,
            @RequestParam(value = "outputHeight", defaultValue = "1600") Integer outputHeight,
            @RequestParam(value = "spacing", defaultValue = "0") Integer spacing,
            @RequestParam(value = "minSpacing", defaultValue = "10") Integer minSpacing,
            @RequestParam(value = "outerPadding", defaultValue = "50") Integer outerPadding,
            @RequestParam(value = "productScale", defaultValue = "80") Integer productScale,
            @RequestParam(value = "cropMargin", defaultValue = "1") Integer cropMargin,
            @RequestParam(value = "skipEmpty", defaultValue = "false") Boolean skipEmpty,
            // 标签设置
            @RequestParam(value = "labelFormat", defaultValue = "×{pcs}") String labelFormat,
            @RequestParam(value = "labelFontSize", defaultValue = "80") Integer labelFontSize,
            @RequestParam(value = "labelPosition", defaultValue = "bottom") String labelPosition,
            @RequestParam(value = "labelMargin", defaultValue = "30") Integer labelMargin,
            @RequestParam(value = "hidePcsOne", defaultValue = "true") Boolean hidePcsOne,
            // 其他设置
            @RequestParam(value = "useCache", defaultValue = "true") Boolean useCache,
            @RequestParam(value = "cacheSize", defaultValue = "100") Integer cacheSize,
            @RequestParam(value = "outputMode", defaultValue = "by_combined_sku") String outputMode,
            @RequestParam(value = "filenamePrefix", defaultValue = "collage/%date:yyyy-MM-dd%/") String filenamePrefix,
            @RequestParam(value = "adaptiveDirection", defaultValue = "auto") String adaptiveDirection
    ) {
        try {
            // 处理 Excel 文件输入
            MultipartFile finalExcelFile = excelFile;

            if (excelFile == null || excelFile.isEmpty()) {
                // 如果没有上传文件，尝试从 URL 下载
                if (excelUrl != null && !excelUrl.trim().isEmpty()) {
                    log.info("从URL下载Excel文件: {}", excelUrl);
                    finalExcelFile = downloadExcelFromUrl(excelUrl.trim());
                } else {
                    return ApiResponse.error("请上传Excel文件或提供Excel文件URL");
                }
            }

            log.info("收到Excel产品拼接请求: {}", finalExcelFile.getOriginalFilename());

            // 构建参数 Map
            Map<String, Object> params = new HashMap<>();

            // Excel配置
            params.put("sheetName", sheetName);
            params.put("combinedSkuCol", combinedSkuCol);
            params.put("skuCol", skuCol);
            params.put("pcsCol", pcsCol);
            params.put("urlCol", urlCol);
            params.put("startRow", startRow);
            params.put("filterCombinedSku", filterCombinedSku);

            // 拼接参数
            params.put("imagesPerCollage", imagesPerCollage);
            params.put("layout", layout);
            params.put("outputWidth", outputWidth);
            params.put("outputHeight", outputHeight);
            params.put("spacing", spacing);
            params.put("minSpacing", minSpacing);
            params.put("outerPadding", outerPadding);
            params.put("productScale", productScale);
            params.put("cropMargin", cropMargin);
            params.put("skipEmpty", skipEmpty);

            // 标签设置
            params.put("labelFormat", labelFormat);
            params.put("labelFontSize", labelFontSize);
            params.put("labelPosition", labelPosition);
            params.put("labelMargin", labelMargin);
            params.put("hidePcsOne", hidePcsOne);

            // 其他设置
            params.put("useCache", useCache);
            params.put("cacheSize", cacheSize);
            params.put("outputMode", outputMode);
            params.put("filenamePrefix", filenamePrefix);
            params.put("adaptiveDirection", adaptiveDirection);

            // 执行拼接
            CollageResult result = comfyUIService.runCollage(finalExcelFile, params);

            if (result.isSuccess()) {
                log.info("Excel产品拼接执行成功，生成 {} 张图片", result.getImageCount());
                return ApiResponse.success(result);
            } else {
                log.error("Excel产品拼接执行失败: {}", result.getErrorMessage());
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Excel产品拼接执行异常", e);
            return ApiResponse.error("Excel产品拼接失败: " + e.getMessage());
        }
    }

    /**
     * 从URL下载Excel文件
     */
    private MultipartFile downloadExcelFromUrl(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                int statusCode = response.getCode();
                if (statusCode != 200) {
                    throw new IOException("下载Excel文件失败，HTTP状态码: " + statusCode);
                }

                byte[] fileBytes = EntityUtils.toByteArray(response.getEntity());

                // 从URL提取文件名
                String filename = "downloaded.xlsx";
                if (url.contains("/")) {
                    String lastPart = url.substring(url.lastIndexOf("/") + 1);
                    if (lastPart.contains("?")) {
                        lastPart = lastPart.substring(0, lastPart.indexOf("?"));
                    }
                    if (lastPart.endsWith(".xlsx") || lastPart.endsWith(".xls")) {
                        filename = lastPart;
                    }
                }

                // 确定content type
                String contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                if (filename.endsWith(".xls")) {
                    contentType = "application/vnd.ms-excel";
                }

                log.info("Excel文件下载成功: {} ({} bytes)", filename, fileBytes.length);

                // 创建 MultipartFile 实现
                final String finalFilename = filename;
                final String finalContentType = contentType;
                return new MultipartFile() {
                    @Override
                    public String getName() {
                        return "excelFile";
                    }

                    @Override
                    public String getOriginalFilename() {
                        return finalFilename;
                    }

                    @Override
                    public String getContentType() {
                        return finalContentType;
                    }

                    @Override
                    public boolean isEmpty() {
                        return fileBytes.length == 0;
                    }

                    @Override
                    public long getSize() {
                        return fileBytes.length;
                    }

                    @Override
                    public byte[] getBytes() throws IOException {
                        return fileBytes;
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return new ByteArrayInputStream(fileBytes);
                    }

                    @Override
                    public void transferTo(File dest) throws IOException, IllegalStateException {
                        java.nio.file.Files.write(dest.toPath(), fileBytes);
                    }
                };
            }
        }
    }
}

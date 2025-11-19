package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.CollageRequest;
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
    public ApiResponse<CollageResult> executeCollage(@ModelAttribute CollageRequest collageRequest) {
        try {
            // 处理 Excel 文件输入
            MultipartFile finalExcelFile = collageRequest.getExcelFile();

            if (finalExcelFile == null || finalExcelFile.isEmpty()) {
                // 如果没有上传文件，尝试从 URL 下载
                String excelUrl = collageRequest.getExcelUrl();
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
            params.put("sheetName", collageRequest.getSheetName());
            params.put("combinedSkuCol", collageRequest.getCombinedSkuCol());
            params.put("skuCol", collageRequest.getSkuCol());
            params.put("pcsCol", collageRequest.getPcsCol());
            params.put("urlCol", collageRequest.getUrlCol());
            params.put("startRow", collageRequest.getStartRow());
            params.put("filterCombinedSku", collageRequest.getFilterCombinedSku());

            // 拼接参数
            params.put("imagesPerCollage", collageRequest.getImagesPerCollage());
            params.put("layout", collageRequest.getLayout());
            params.put("outputWidth", collageRequest.getOutputWidth());
            params.put("outputHeight", collageRequest.getOutputHeight());
            params.put("spacing", collageRequest.getSpacing());
            params.put("minSpacing", collageRequest.getMinSpacing());
            params.put("outerPadding", collageRequest.getOuterPadding());
            params.put("productScale", collageRequest.getProductScale());
            params.put("cropMargin", collageRequest.getCropMargin());
            params.put("skipEmpty", collageRequest.getSkipEmpty());

            // 标签设置
            params.put("labelFormat", collageRequest.getLabelFormat());
            params.put("labelFontSize", collageRequest.getLabelFontSize());
            params.put("labelPosition", collageRequest.getLabelPosition());
            params.put("labelMargin", collageRequest.getLabelMargin());
            params.put("hidePcsOne", collageRequest.getHidePcsOne());

            // 其他设置
            params.put("useCache", collageRequest.getUseCache());
            params.put("cacheSize", collageRequest.getCacheSize());
            params.put("outputMode", collageRequest.getOutputMode());
            params.put("filenamePrefix", collageRequest.getFilenamePrefix());
            params.put("adaptiveDirection", collageRequest.getAdaptiveDirection());

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

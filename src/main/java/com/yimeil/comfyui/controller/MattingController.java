package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.BatchMattingRequest;
import com.yimeil.comfyui.model.BatchMattingResult;
import com.yimeil.comfyui.model.KeywordMattingRequest;
import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import com.yimeil.comfyui.service.ComfyUIService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 抠图 API 控制器
 */
@RestController
@RequestMapping("/api/matting")
@Slf4j
public class MattingController {

    @Autowired
    private ComfyUIService comfyUIService;

    /**
     * 执行抠图
     */
    @PostMapping("/execute")
    public ApiResponse<MattingResult> executeMatting(@ModelAttribute MattingRequest request) {
        try {
            log.info("收到抠图请求: {}", request.getImage().getOriginalFilename());

            // 执行抠图
            MattingResult result = comfyUIService.runMatting(request.getImage(), request);

            if (result.isSuccess()) {
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("抠图失败", e);
            return ApiResponse.error("抠图失败: " + e.getMessage());
        }
    }

    /**
     * 执行关键字抠图
     */
    @PostMapping("/keyword")
    public ApiResponse<MattingResult> executeKeywordMatting(@ModelAttribute KeywordMattingRequest request) {
        try {
            log.info("收到关键字抠图请求: {}, 关键字: {}",
                    request.getImage().getOriginalFilename(), request.getKeyword());

            // 执行关键字抠图
            MattingResult result = comfyUIService.runKeywordMatting(request);

            if (result.isSuccess()) {
                log.info("关键字抠图执行成功: {}", result.getOutputFilename());
                return ApiResponse.success(result);
            } else {
                log.error("关键字抠图执行失败: {}", result.getErrorMessage());
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("关键字抠图执行异常", e);
            return ApiResponse.error("关键字抠图失败: " + e.getMessage());
        }
    }

    /**
     * 执行BiRefNet批量抠图
     */
    @PostMapping("/batch")
    public ApiResponse<BatchMattingResult> executeBatchMatting(@ModelAttribute BatchMattingRequest request) {
        try {
            String inputMode = request.getInputMode() != null ? request.getInputMode() : "image";
            log.info("收到BiRefNet批量抠图请求: 模式={}", inputMode);

            // 根据输入模式验证参数
            switch (inputMode) {
                case "urls":
                    if (request.getImageUrls() == null || request.getImageUrls().trim().isEmpty()) {
                        return ApiResponse.error("请提供图片URL地址");
                    }
                    log.info("URL模式 - 地址数量: {}", request.getImageUrls().split("\n").length);
                    break;

                case "zip":
                    if (request.getZipFile() == null || request.getZipFile().isEmpty()) {
                        return ApiResponse.error("请上传压缩文件");
                    }
                    log.info("ZIP模式 - 文件名: {}", request.getZipFile().getOriginalFilename());
                    break;

                case "image":
                default:
                    if (request.getImageFile() == null || request.getImageFile().isEmpty()) {
                        return ApiResponse.error("请上传图片文件");
                    }
                    log.info("图片上传模式 - 文件名: {}", request.getImageFile().getOriginalFilename());
                    break;
            }

            // 执行批量抠图
            BatchMattingResult result = comfyUIService.runBatchMatting(request);

            if (result.isSuccess()) {
                log.info("BiRefNet批量抠图执行成功，生成 {} 张图片", result.getImageCount());
                return ApiResponse.success(result);
            } else {
                log.error("BiRefNet批量抠图执行失败: {}", result.getErrorMessage());
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("BiRefNet批量抠图执行异常", e);
            return ApiResponse.error("BiRefNet批量抠图失败: " + e.getMessage());
        }
    }

    /**
     * 检查服务器状态
     */
    @GetMapping("/status")
    public ApiResponse<Boolean> checkStatus() {
        boolean status = comfyUIService.checkServerStatus();
        if (status) {
            return ApiResponse.success(true);
        } else {
            return ApiResponse.error("ComfyUI 服务器未连接");
        }
    }

    /**
     * 代理下载图片（解决前端跨域问题）
     */
    @GetMapping("/download")
    public void downloadImage(@RequestParam String url,
                              @RequestParam String filename, HttpServletResponse response) {
        try {
            log.info("代理下载图片: url={}, filename={}", url, filename);

            // 从 ComfyUI 服务器下载图片
            org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient =
                    org.apache.hc.client5.http.impl.classic.HttpClients.createDefault();
            org.apache.hc.client5.http.classic.methods.HttpGet httpGet =
                    new org.apache.hc.client5.http.classic.methods.HttpGet(url);

            org.apache.hc.client5.http.impl.classic.CloseableHttpResponse httpResponse =
                    httpClient.execute(httpGet);

            // 获取图片数据
            byte[] imageBytes = org.apache.hc.core5.http.io.entity.EntityUtils.toByteArray(
                    httpResponse.getEntity());

            // 设置响应头，处理中文文件名编码
            response.setContentType("application/octet-stream");
            String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFilename);
            response.setContentLength(imageBytes.length);

            // 写入响应
            response.getOutputStream().write(imageBytes);
            response.getOutputStream().flush();

            httpResponse.close();
            httpClient.close();

            log.info("图片下载成功: {}", filename);

        } catch (Exception e) {
            log.error("代理下载图片失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}

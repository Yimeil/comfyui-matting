package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.MattingResult;
import com.yimeil.comfyui.model.QwenEditRequest;
import com.yimeil.comfyui.model.QwenTranslateRequest;
import com.yimeil.comfyui.service.ComfyUIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Qwen AI 图像处理 API 控制器
 */
@RestController
@RequestMapping("/api/qwen")
@Slf4j
public class QwenController {

    @Autowired
    private ComfyUIService comfyUIService;

    /**
     * 执行Qwen多角度编辑
     * 使用工作流: qwen-Edit-Multiple-angles-api.json
     */
    @PostMapping("/edit-angles")
    public ApiResponse<MattingResult> editMultipleAngles(@ModelAttribute QwenEditRequest request) {
        try {
            log.info("收到Qwen多角度编辑请求: {}, 编辑指令: {}",
                    request.getImage().getOriginalFilename(), request.getEditInstruction());

            // 验证必需参数
            if (request.getImage() == null || request.getImage().isEmpty()) {
                return ApiResponse.error("请上传图片文件");
            }

            // 执行Qwen编辑
            MattingResult result = comfyUIService.runQwenEdit(request);

            if (result.isSuccess()) {
                log.info("Qwen多角度编辑执行成功: {}", result.getOutputFilename());
                return ApiResponse.success(result);
            } else {
                log.error("Qwen多角度编辑执行失败: {}", result.getErrorMessage());
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Qwen多角度编辑执行异常", e);
            return ApiResponse.error("Qwen多角度编辑失败: " + e.getMessage());
        }
    }

    /**
     * 执行Qwen图像翻译
     * 使用工作流: qwen-img-translate-api.json
     */
    @PostMapping("/translate-image")
    public ApiResponse<MattingResult> translateImage(@ModelAttribute QwenTranslateRequest request) {
        try {
            log.info("收到Qwen图像翻译请求: {}, 源语言: {}, 目标语言: {}",
                    request.getImage() != null ? request.getImage().getOriginalFilename() : "URL",
                    request.getSourceLang(), request.getTargetLang());

            // 验证必需参数
            if ((request.getImage() == null || request.getImage().isEmpty()) &&
                    (request.getImageUrl() == null || request.getImageUrl().trim().isEmpty())) {
                return ApiResponse.error("请上传图片文件或提供图片URL");
            }

            if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
                return ApiResponse.error("请提供Qwen API密钥");
            }

            // 执行Qwen翻译
            MattingResult result = comfyUIService.runQwenTranslate(request);

            if (result.isSuccess()) {
                log.info("Qwen图像翻译执行成功: {}", result.getOutputFilename());
                return ApiResponse.success(result);
            } else {
                log.error("Qwen图像翻译执行失败: {}", result.getErrorMessage());
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Qwen图像翻译执行异常", e);
            return ApiResponse.error("Qwen图像翻译失败: " + e.getMessage());
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
}

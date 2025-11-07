package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import com.yimeil.comfyui.service.ComfyUIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ApiResponse<MattingResult> executeMatting(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "workflowName", required = false) String workflowName,
            @RequestParam(value = "threshold", required = false) Double threshold,
            @RequestParam(value = "alphaMatting", required = false) Boolean alphaMatting,
            @RequestParam(value = "alphaMattingForegroundThreshold", required = false) Integer alphaMattingForegroundThreshold,
            @RequestParam(value = "alphaMattingBackgroundThreshold", required = false) Integer alphaMattingBackgroundThreshold,
            @RequestParam(value = "alphaMattingErodeSize", required = false) Integer alphaMattingErodeSize
    ) {
        try {
            log.info("收到抠图请求: {}", imageFile.getOriginalFilename());

            // 构建请求
            MattingRequest request = new MattingRequest();
            request.setWorkflowName(workflowName);
            request.setThreshold(threshold);
            request.setAlphaMatting(alphaMatting);
            request.setAlphaMattingForegroundThreshold(alphaMattingForegroundThreshold);
            request.setAlphaMattingBackgroundThreshold(alphaMattingBackgroundThreshold);
            request.setAlphaMattingErodeSize(alphaMattingErodeSize);

            // 执行抠图
            MattingResult result = comfyUIService.runMatting(imageFile, request);

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

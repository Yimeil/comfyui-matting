package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.KeywordMattingRequest;
import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import com.yimeil.comfyui.service.ComfyUIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    public ApiResponse<MattingResult> executeKeywordMatting(@ModelAttribute KeywordMattingRequest keywordRequest) {
        try {
            log.info("收到关键字抠图请求: {}, 关键字: {}",
                    keywordRequest.getImage().getOriginalFilename(), keywordRequest.getKeyword());

            // 构建参数 Map
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keywordRequest.getKeyword());
            params.put("translateFrom", keywordRequest.getTranslateFrom());
            params.put("samModel", keywordRequest.getSamModel());
            params.put("dinoModel", keywordRequest.getDinoModel());
            params.put("threshold", keywordRequest.getThreshold());
            params.put("detailMethod", keywordRequest.getDetailMethod());
            params.put("detailErode", keywordRequest.getDetailErode());
            params.put("detailDilate", keywordRequest.getDetailDilate());
            params.put("blackPoint", keywordRequest.getBlackPoint());
            params.put("whitePoint", keywordRequest.getWhitePoint());
            params.put("maxMegapixels", keywordRequest.getMaxMegapixels());
            params.put("device", keywordRequest.getDevice());

            // 构建请求
            MattingRequest request = new MattingRequest();
            request.setWorkflowName("matting_keyword_api.json");

            // 执行关键字抠图
            MattingResult result = comfyUIService.runKeywordMatting(keywordRequest.getImage(), request, params);

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

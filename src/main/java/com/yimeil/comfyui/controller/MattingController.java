package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import com.yimeil.comfyui.service.ComfyUIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * 执行关键字抠图
     */
    @PostMapping("/keyword")
    public ApiResponse<MattingResult> executeKeywordMatting(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "translateFrom", defaultValue = "chinese") String translateFrom,
            @RequestParam(value = "samModel", defaultValue = "sam_vit_h (2.56GB)") String samModel,
            @RequestParam(value = "dinoModel", defaultValue = "GroundingDINO_SwinT_OGC (694MB)") String dinoModel,
            @RequestParam(value = "threshold", defaultValue = "0.3") Double threshold,
            @RequestParam(value = "detailMethod", defaultValue = "VITMatte(local)") String detailMethod,
            @RequestParam(value = "detailErode", defaultValue = "6") Integer detailErode,
            @RequestParam(value = "detailDilate", defaultValue = "6") Integer detailDilate,
            @RequestParam(value = "blackPoint", defaultValue = "0.15") Double blackPoint,
            @RequestParam(value = "whitePoint", defaultValue = "0.99") Double whitePoint,
            @RequestParam(value = "maxMegapixels", defaultValue = "2.0") Double maxMegapixels,
            @RequestParam(value = "device", defaultValue = "cuda") String device
    ) {
        try {
            log.info("收到关键字抠图请求: {}, 关键字: {}", imageFile.getOriginalFilename(), keyword);

            // 构建参数 Map
            Map<String, Object> params = new HashMap<>();
            params.put("keyword", keyword);
            params.put("translateFrom", translateFrom);
            params.put("samModel", samModel);
            params.put("dinoModel", dinoModel);
            params.put("threshold", threshold);
            params.put("detailMethod", detailMethod);
            params.put("detailErode", detailErode);
            params.put("detailDilate", detailDilate);
            params.put("blackPoint", blackPoint);
            params.put("whitePoint", whitePoint);
            params.put("maxMegapixels", maxMegapixels);
            params.put("device", device);

            // 构建请求
            MattingRequest request = new MattingRequest();
            request.setWorkflowName("matting_keyword_api.json");

            // 执行关键字抠图
            MattingResult result = comfyUIService.runKeywordMatting(imageFile, request, params);

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

package com.comfyui.matting.controller;

import com.comfyui.matting.model.MattingRequest;
import com.comfyui.matting.service.ComfyUIService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 图像抠图 REST API 控制器
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 允许跨域访问
public class MattingController {

    @Autowired
    private ComfyUIService comfyUIService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        boolean comfyuiStatus = comfyUIService.checkServerStatus();

        response.put("status", "ok");
        response.put("comfyui_connected", comfyuiStatus);

        return ResponseEntity.ok(response);
    }

    /**
     * 上传并处理图像
     */
    @PostMapping("/matting")
    public ResponseEntity<Map<String, Object>> processMatting(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam("mask") MultipartFile maskFile,
            @RequestParam(value = "maskHintThreshold", required = false, defaultValue = "0.6") Double maskHintThreshold,
            @RequestParam(value = "kernelSize", required = false, defaultValue = "6") Integer kernelSize,
            @RequestParam(value = "expand", required = false, defaultValue = "-3") Integer expand,
            @RequestParam(value = "blurRadius", required = false, defaultValue = "1.0") Double blurRadius,
            @RequestParam(value = "preset", required = false, defaultValue = "default") String preset) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 1. 上传图像
            String imageFilename = comfyUIService.uploadImage(imageFile);
            String maskFilename = comfyUIService.uploadImage(maskFile);

            // 2. 构建参数
            MattingRequest params = new MattingRequest();
            params.setMaskHintThreshold(maskHintThreshold);
            params.setKernelSize(kernelSize);
            params.setExpand(expand);
            params.setBlurRadius(blurRadius);
            params.setPreset(preset);

            // 3. 提交工作流
            String promptId = comfyUIService.queuePrompt(imageFilename, maskFilename, params);

            response.put("success", true);
            response.put("prompt_id", promptId);
            response.put("message", "工作流已提交，请轮询结果");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取处理结果
     */
    @GetMapping("/result/{promptId}")
    public ResponseEntity<Map<String, Object>> getResult(@PathVariable String promptId) {
        Map<String, Object> response = new HashMap<>();

        try {
            JsonNode history = comfyUIService.getHistory(promptId);

            if (history.has(promptId)) {
                JsonNode promptData = history.get(promptId);
                JsonNode outputs = promptData.get("outputs");

                // 检查是否完成
                if (outputs != null && outputs.has("22")) {
                    JsonNode images = outputs.get("22").get("images");

                    if (images.isArray() && images.size() > 0) {
                        JsonNode firstImage = images.get(0);
                        String filename = firstImage.get("filename").asText();
                        String subfolder = firstImage.has("subfolder") ?
                                firstImage.get("subfolder").asText() : "";

                        response.put("success", true);
                        response.put("status", "completed");
                        response.put("filename", filename);
                        response.put("subfolder", subfolder);
                    } else {
                        response.put("success", true);
                        response.put("status", "processing");
                    }
                } else {
                    response.put("success", true);
                    response.put("status", "processing");
                }
            } else {
                response.put("success", false);
                response.put("error", "Prompt ID not found");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 下载结果图像
     */
    @GetMapping("/download/{filename}")
    public ResponseEntity<byte[]> downloadImage(
            @PathVariable String filename,
            @RequestParam(required = false, defaultValue = "") String subfolder) {

        try {
            byte[] imageData = comfyUIService.downloadImage(filename, subfolder);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(imageData, headers, HttpStatus.OK);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

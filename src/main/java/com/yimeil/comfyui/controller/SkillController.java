package com.yimeil.comfyui.controller;

import com.yimeil.comfyui.model.ApiResponse;
import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import com.yimeil.comfyui.service.SkillExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * Claude Skills API 控制器
 *
 * 处理前端调用 Claude Skills 的请求
 * 流程: Vue前端 → /api/skill/* → SkillExecutor → Claude Skill → ComfyUI API
 */
@RestController
@RequestMapping("/api/skill")
@Slf4j
public class SkillController {

    @Autowired
    private SkillExecutor skillExecutor;

    /**
     * 执行抠图 Skill
     *
     * POST /api/skill/matting
     *
     * @param imageFile 上传的图片文件
     * @param workflowName 工作流名称（可选）
     * @param threshold SAM检测阈值（可选）
     * @param alphaMatting 是否启用边缘优化（可选）
     * @param alphaMattingForegroundThreshold 前景阈值（可选）
     * @param alphaMattingBackgroundThreshold 背景阈值（可选）
     * @param alphaMattingErodeSize 边缘腐蚀大小（可选）
     * @return 抠图结果
     */
    @PostMapping("/matting")
    public ApiResponse<MattingResult> executeMattingSkill(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(value = "workflowName", required = false) String workflowName,
            @RequestParam(value = "threshold", required = false) Double threshold,
            @RequestParam(value = "alphaMatting", required = false) Boolean alphaMatting,
            @RequestParam(value = "alphaMattingForegroundThreshold", required = false) Integer alphaMattingForegroundThreshold,
            @RequestParam(value = "alphaMattingBackgroundThreshold", required = false) Integer alphaMattingBackgroundThreshold,
            @RequestParam(value = "alphaMattingErodeSize", required = false) Integer alphaMattingErodeSize
    ) {
        try {
            log.info("收到 Matting Skill 请求: {}", imageFile.getOriginalFilename());

            // 构建请求参数
            MattingRequest request = new MattingRequest();
            request.setWorkflowName(workflowName);
            request.setThreshold(threshold);
            request.setAlphaMatting(alphaMatting);
            request.setAlphaMattingForegroundThreshold(alphaMattingForegroundThreshold);
            request.setAlphaMattingBackgroundThreshold(alphaMattingBackgroundThreshold);
            request.setAlphaMattingErodeSize(alphaMattingErodeSize);

            // 执行 Matting Skill
            MattingResult result = skillExecutor.executeMattingSkill(imageFile, request);

            if (result.isSuccess()) {
                log.info("Matting Skill 执行成功: {}", result.getOutputFilename());
                return ApiResponse.success(result);
            } else {
                log.error("Matting Skill 执行失败: {}", result.getErrorMessage());
                return ApiResponse.error(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Matting Skill 执行异常", e);
            return ApiResponse.error("Skill 执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取可用的 Skills 列表
     *
     * GET /api/skill/list
     *
     * @return Skills 列表
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> listSkills() {
        try {
            Map<String, Object> skills = new HashMap<>();

            // Matting Skill 信息
            Map<String, Object> mattingSkill = new HashMap<>();
            mattingSkill.put("name", "matting");
            mattingSkill.put("displayName", "智能抠图");
            mattingSkill.put("description", "使用 SAM 模型进行智能图像抠图");
            mattingSkill.put("endpoint", "/api/skill/matting");
            mattingSkill.put("method", "POST");
            mattingSkill.put("status", "available");

            skills.put("matting", mattingSkill);

            return ApiResponse.success(skills);
        } catch (Exception e) {
            log.error("获取 Skills 列表失败", e);
            return ApiResponse.error("获取 Skills 列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取特定 Skill 的信息
     *
     * GET /api/skill/{skillName}/info
     *
     * @param skillName Skill 名称
     * @return Skill 信息
     */
    @GetMapping("/{skillName}/info")
    public ApiResponse<Map<String, Object>> getSkillInfo(@PathVariable String skillName) {
        try {
            if ("matting".equals(skillName)) {
                Map<String, Object> info = new HashMap<>();
                info.put("name", "matting");
                info.put("displayName", "智能抠图");
                info.put("description", "使用 Segment Anything Model (SAM) 进行智能图像抠图");
                info.put("version", "1.0.0");
                info.put("parameters", Map.of(
                    "image", "必需 - 上传的图片文件",
                    "threshold", "可选 - SAM 检测阈值 (0.0-1.0), 默认: 0.3",
                    "alphaMatting", "可选 - 是否启用边缘优化, 默认: true",
                    "alphaMattingForegroundThreshold", "可选 - 前景阈值 (200-255), 默认: 240",
                    "alphaMattingBackgroundThreshold", "可选 - 背景阈值 (0-50), 默认: 10",
                    "alphaMattingErodeSize", "可选 - 边缘腐蚀大小 (0-20), 默认: 10"
                ));

                return ApiResponse.success(info);
            } else {
                return ApiResponse.error("Skill 不存在: " + skillName);
            }
        } catch (Exception e) {
            log.error("获取 Skill 信息失败", e);
            return ApiResponse.error("获取 Skill 信息失败: " + e.getMessage());
        }
    }
}

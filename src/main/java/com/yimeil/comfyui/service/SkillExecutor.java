package com.yimeil.comfyui.service;

import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Claude Skill 执行器
 *
 * 负责执行 .claude/skills/ 目录下定义的各种 skills
 * 每个 skill 通过调用 ComfyUIService 来完成具体任务
 *
 * 架构流程:
 * Vue前端 → SkillController → SkillExecutor → ComfyUIService → ComfyUI API
 */
@Service
@Slf4j
public class SkillExecutor {

    @Autowired
    private ComfyUIService comfyUIService;

    /**
     * 执行 Matting Skill
     *
     * 对应 .claude/skills/matting.md
     *
     * @param imageFile 上传的图片文件
     * @param request 抠图请求参数
     * @return 抠图结果
     */
    public MattingResult executeMattingSkill(MultipartFile imageFile, MattingRequest request) {
        log.info("【Matting Skill】开始执行");
        log.info("【Matting Skill】输入文件: {}", imageFile.getOriginalFilename());
        log.info("【Matting Skill】参数: threshold={}, alphaMatting={}",
                request.getThreshold(), request.getAlphaMatting());

        MattingResult result = new MattingResult();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: 验证 Skill 定义文件存在
            validateSkillExists("matting");

            // Step 2: 验证输入参数
            validateMattingParameters(request);

            // Step 3: 调用 ComfyUIService 执行实际的抠图任务
            log.info("【Matting Skill】调用 ComfyUIService 执行抠图");
            result = comfyUIService.runMatting(imageFile, request);

            // Step 4: 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTime(executionTime);

            if (result.isSuccess()) {
                log.info("【Matting Skill】执行成功，耗时: {}ms", executionTime);
                log.info("【Matting Skill】输出文件: {}", result.getOutputFilename());
            } else {
                log.error("【Matting Skill】执行失败: {}", result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("【Matting Skill】执行异常", e);
            result.setSuccess(false);
            result.setErrorMessage("Matting Skill 执行失败: " + e.getMessage());
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * 验证 Skill 定义文件是否存在
     *
     * @param skillName Skill 名称
     * @throws Exception 如果 skill 不存在
     */
    private void validateSkillExists(String skillName) throws Exception {
        Path skillPath = Paths.get(".claude/skills/" + skillName + ".md");
        if (!Files.exists(skillPath)) {
            throw new Exception("Skill 定义文件不存在: " + skillPath);
        }
        log.info("【Skill Validator】找到 Skill 定义: {}", skillPath);
    }

    /**
     * 验证 Matting 参数的有效性
     *
     * @param request 请求参数
     * @throws Exception 如果参数无效
     */
    private void validateMattingParameters(MattingRequest request) throws Exception {
        // 验证 threshold
        if (request.getThreshold() != null) {
            double threshold = request.getThreshold();
            if (threshold < 0.0 || threshold > 1.0) {
                throw new Exception("threshold 必须在 0.0 到 1.0 之间，当前值: " + threshold);
            }
        }

        // 验证 alphaMattingForegroundThreshold
        if (request.getAlphaMattingForegroundThreshold() != null) {
            int value = request.getAlphaMattingForegroundThreshold();
            if (value < 200 || value > 255) {
                throw new Exception("alphaMattingForegroundThreshold 必须在 200 到 255 之间，当前值: " + value);
            }
        }

        // 验证 alphaMattingBackgroundThreshold
        if (request.getAlphaMattingBackgroundThreshold() != null) {
            int value = request.getAlphaMattingBackgroundThreshold();
            if (value < 0 || value > 50) {
                throw new Exception("alphaMattingBackgroundThreshold 必须在 0 到 50 之间，当前值: " + value);
            }
        }

        // 验证 alphaMattingErodeSize
        if (request.getAlphaMattingErodeSize() != null) {
            int value = request.getAlphaMattingErodeSize();
            if (value < 0 || value > 20) {
                throw new Exception("alphaMattingErodeSize 必须在 0 到 20 之间，当前值: " + value);
            }
        }

        log.info("【Skill Validator】参数验证通过");
    }

    /**
     * 执行关键字抠图 Skill
     *
     * @param imageFile 上传的图片文件
     * @param params 关键字抠图参数
     * @return 抠图结果
     */
    public MattingResult executeKeywordMattingSkill(MultipartFile imageFile, Map<String, Object> params) {
        log.info("【Keyword Matting Skill】开始执行");
        log.info("【Keyword Matting Skill】输入文件: {}", imageFile.getOriginalFilename());
        log.info("【Keyword Matting Skill】关键字: {}", params.get("keyword"));

        MattingResult result = new MattingResult();
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: 验证 Skill 定义文件存在
            validateSkillExists("keyword-matting");

            // Step 2: 验证关键字参数
            validateKeywordMattingParameters(params);

            // Step 3: 构建 MattingRequest（用于工作流名称）
            MattingRequest request = new MattingRequest();
            request.setWorkflowName("matting_keyword_api.json");

            // Step 4: 调用 ComfyUIService 执行关键字抠图
            log.info("【Keyword Matting Skill】调用 ComfyUIService 执行关键字抠图");
            result = comfyUIService.runKeywordMatting(imageFile, request, params);

            // Step 5: 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTime(executionTime);

            if (result.isSuccess()) {
                log.info("【Keyword Matting Skill】执行成功，耗时: {}ms", executionTime);
                log.info("【Keyword Matting Skill】输出文件: {}", result.getOutputFilename());
            } else {
                log.error("【Keyword Matting Skill】执行失败: {}", result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("【Keyword Matting Skill】执行异常", e);
            result.setSuccess(false);
            result.setErrorMessage("Keyword Matting Skill 执行失败: " + e.getMessage());
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * 验证关键字抠图参数
     *
     * @param params 参数 Map
     * @throws Exception 如果参数无效
     */
    private void validateKeywordMattingParameters(Map<String, Object> params) throws Exception {
        // 验证关键字是否存在
        String keyword = (String) params.get("keyword");
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new Exception("关键字不能为空");
        }

        // 验证 threshold
        if (params.containsKey("threshold")) {
            double threshold = ((Number) params.get("threshold")).doubleValue();
            if (threshold < 0.1 || threshold > 1.0) {
                throw new Exception("threshold 必须在 0.1 到 1.0 之间，当前值: " + threshold);
            }
        }

        // 验证 detailErode
        if (params.containsKey("detailErode")) {
            int value = ((Number) params.get("detailErode")).intValue();
            if (value < 0 || value > 20) {
                throw new Exception("detailErode 必须在 0 到 20 之间，当前值: " + value);
            }
        }

        // 验证 detailDilate
        if (params.containsKey("detailDilate")) {
            int value = ((Number) params.get("detailDilate")).intValue();
            if (value < 0 || value > 20) {
                throw new Exception("detailDilate 必须在 0 到 20 之间，当前值: " + value);
            }
        }

        log.info("【Skill Validator】关键字抠图参数验证通过");
    }

    /**
     * 获取 Skill 的状态
     *
     * @param skillName Skill 名称
     * @return Skill 是否可用
     */
    public boolean isSkillAvailable(String skillName) {
        try {
            Path skillPath = Paths.get(".claude/skills/" + skillName + ".md");
            return Files.exists(skillPath);
        } catch (Exception e) {
            log.error("检查 Skill 状态失败: {}", skillName, e);
            return false;
        }
    }

    /**
     * 读取 Skill 定义文件内容
     *
     * @param skillName Skill 名称
     * @return Skill 定义内容
     */
    public String getSkillDefinition(String skillName) {
        try {
            Path skillPath = Paths.get(".claude/skills/" + skillName + ".md");
            if (Files.exists(skillPath)) {
                return Files.readString(skillPath);
            }
            return null;
        } catch (Exception e) {
            log.error("读取 Skill 定义失败: {}", skillName, e);
            return null;
        }
    }
}

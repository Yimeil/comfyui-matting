package com.yimeil.comfyui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yimeil.comfyui.config.ComfyUIConfig;
import com.yimeil.comfyui.model.MattingRequest;
import com.yimeil.comfyui.model.MattingResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * ComfyUI 服务
 * 类似 word2picture 项目的 ComfyUIService
 */
@Service
@Slf4j
public class ComfyUIService {

    @Autowired
    private ComfyUIConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId = UUID.randomUUID().toString();

    /**
     * 从 resources/workflow 加载工作流
     * 类似 word2picture 的 loadWorkflowFromResource 方法
     */
    public JsonNode loadWorkflowFromResource(String workflowName) throws IOException {
        log.info("加载工作流: {}", workflowName);

        String workflowPath = config.getWorkflow().getDirectory() + "/" + workflowName;
        ClassPathResource resource = new ClassPathResource(workflowPath);

        if (!resource.exists()) {
            throw new IOException("工作流文件不存在: " + workflowPath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    /**
     * 上传图片到 ComfyUI 服务器
     */
    public String uploadImage(MultipartFile file) throws IOException {
        log.info("上传图片: {}", file.getOriginalFilename());

        String url = config.getApi().getBaseUrl() + "/upload/image";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            // 构建 multipart 请求
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("image", file.getInputStream(),
                            ContentType.APPLICATION_OCTET_STREAM,
                            file.getOriginalFilename())
                    .addTextBody("overwrite", "true")
                    .build();

            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                String uploadedName = jsonNode.get("name").asText();

                log.info("图片上传成功: {}", uploadedName);
                return uploadedName;
            }
        }
    }

    /**
     * 更新工作流参数
     */
    public JsonNode updateWorkflowParams(JsonNode workflow, String nodeId,
                                         String paramName, Object paramValue) {
        log.debug("更新工作流参数: 节点{} -> {} = {}", nodeId, paramName, paramValue);

        ObjectNode workflowObj = (ObjectNode) workflow;
        if (workflowObj.has(nodeId)) {
            ObjectNode nodeObj = (ObjectNode) workflowObj.get(nodeId);
            if (nodeObj.has("inputs")) {
                ObjectNode inputsObj = (ObjectNode) nodeObj.get("inputs");
                if (paramValue instanceof String) {
                    inputsObj.put(paramName, (String) paramValue);
                } else if (paramValue instanceof Integer) {
                    inputsObj.put(paramName, (Integer) paramValue);
                } else if (paramValue instanceof Double) {
                    inputsObj.put(paramName, (Double) paramValue);
                } else if (paramValue instanceof Boolean) {
                    inputsObj.put(paramName, (Boolean) paramValue);
                }
            }
        }

        return workflowObj;
    }

    /**
     * 执行工作流
     */
    public String executeWorkflow(JsonNode workflow) throws IOException {
        log.info("提交工作流执行...");

        String url = config.getApi().getBaseUrl() + "/prompt";

        // 构建请求 payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("prompt", workflow);
        payload.put("client_id", clientId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(payload)));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // 检查是否有错误
                if (jsonNode.has("error")) {
                    JsonNode error = jsonNode.get("error");
                    String errorType = error.has("type") ? error.get("type").asText() : "unknown";
                    String errorMessage = error.has("message") ? error.get("message").asText() : responseBody;
                    log.error("ComfyUI 工作流执行错误 [{}]: {}", errorType, errorMessage);
                    throw new IOException("ComfyUI 错误 [" + errorType + "]: " + errorMessage);
                }

                // 获取 prompt_id
                if (!jsonNode.has("prompt_id")) {
                    log.error("ComfyUI 响应中缺少 prompt_id: {}", responseBody);
                    throw new IOException("ComfyUI 响应格式错误: 缺少 prompt_id");
                }

                String promptId = jsonNode.get("prompt_id").asText();
                log.info("工作流已提交，Prompt ID: {}", promptId);
                return promptId;
            }
        }
    }

    /**
     * 等待执行完成并获取结果
     */
    public JsonNode waitForCompletion(String promptId) throws IOException, InterruptedException {
        log.info("等待工作流执行完成...");

        String url = config.getApi().getBaseUrl() + "/history/" + promptId;

        // 轮询检查执行状态（最多等待 5 分钟）
        int maxAttempts = 60;
        int attempt = 0;

        while (attempt < maxAttempts) {
            Thread.sleep(5000); // 每 5 秒检查一次

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    JsonNode history = objectMapper.readTree(responseBody);

                    if (history.has(promptId)) {
                        log.info("工作流执行完成");
                        return history.get(promptId).get("outputs");
                    }
                }
            }

            attempt++;
        }

        throw new IOException("工作流执行超时");
    }

    /**
     * 下载输出图片
     */
    public File downloadImage(String filename, String subfolder, String outputDir) throws IOException {
        log.info("下载图片: {}", filename);

        String url = config.getApi().getBaseUrl() + "/view"
                + "?filename=" + filename
                + "&subfolder=" + (subfolder != null ? subfolder : "")
                + "&type=output";

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                byte[] imageBytes = EntityUtils.toByteArray(response.getEntity());

                // 保存到输出目录
                Path outputPath = Paths.get(outputDir);
                if (!Files.exists(outputPath)) {
                    Files.createDirectories(outputPath);
                }

                File outputFile = new File(outputPath.toFile(), filename);
                FileUtils.writeByteArrayToFile(outputFile, imageBytes);

                log.info("图片已保存: {}", outputFile.getAbsolutePath());
                return outputFile;
            }
        }
    }

    /**
     * 一键执行抠图
     * 简化的 API，类似 Python 版本的 run_matting
     */
    public MattingResult runMatting(MultipartFile imageFile, MattingRequest request) {
        MattingResult result = new MattingResult();
        long startTime = System.currentTimeMillis();

        try {
            // 1. 加载工作流
            String workflowName = request.getWorkflowName() != null ?
                    request.getWorkflowName() : config.getWorkflow().getDefaultWorkflow();
            JsonNode workflow = loadWorkflowFromResource(workflowName);

            // 2. 上传图片
            String uploadedName = uploadImage(imageFile);

            // 3. 更新工作流参数
            workflow = updateWorkflowParams(workflow, "10", "image", uploadedName);

            // 应用自定义参数
            if (request.getThreshold() != null) {
                workflow = updateWorkflowParams(workflow, "15", "threshold", request.getThreshold());
            }

            if (request.getAlphaMatting() != null && request.getAlphaMatting()) {
                workflow = updateWorkflowParams(workflow, "23", "alpha_matting", "true");
                if (request.getAlphaMattingForegroundThreshold() != null) {
                    workflow = updateWorkflowParams(workflow, "23",
                            "alpha_matting_foreground_threshold",
                            request.getAlphaMattingForegroundThreshold());
                }
                if (request.getAlphaMattingBackgroundThreshold() != null) {
                    workflow = updateWorkflowParams(workflow, "23",
                            "alpha_matting_background_threshold",
                            request.getAlphaMattingBackgroundThreshold());
                }
                if (request.getAlphaMattingErodeSize() != null) {
                    workflow = updateWorkflowParams(workflow, "23",
                            "alpha_matting_erode_size",
                            request.getAlphaMattingErodeSize());
                }
            }

            // 4. 执行工作流
            String promptId = executeWorkflow(workflow);
            result.setPromptId(promptId);

            // 5. 等待完成
            JsonNode outputs = waitForCompletion(promptId);

            // 6. 下载结果
            String outputFilename = null;
            for (JsonNode nodeOutput : outputs) {
                if (nodeOutput.has("images")) {
                    JsonNode images = nodeOutput.get("images");
                    if (images.isArray() && images.size() > 0) {
                        JsonNode firstImage = images.get(0);
                        String filename = firstImage.get("filename").asText();
                        String subfolder = firstImage.has("subfolder") ?
                                firstImage.get("subfolder").asText() : "";

                        downloadImage(filename, subfolder, "output");
                        outputFilename = filename;
                        break;
                    }
                }
            }

            if (outputFilename != null) {
                result.setSuccess(true);
                result.setOutputFilename(outputFilename);
                result.setOutputUrl("/output/" + outputFilename);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("未找到输出图片");
            }

        } catch (Exception e) {
            log.error("抠图失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        result.setExecutionTime(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 关键字抠图
     * 使用文本关键字识别并提取图像中的对象
     */
    public MattingResult runKeywordMatting(MultipartFile imageFile, MattingRequest request) {
        MattingResult result = new MattingResult();
        long startTime = System.currentTimeMillis();

        try {
            log.info("开始关键字抠图，关键字: {}", request.getKeyword());

            // 1. 加载关键字抠图工作流
            JsonNode workflow = loadWorkflowFromResource("keyword_matting.json");

            // 2. 上传图片
            String uploadedName = uploadImage(imageFile);

            // 3. 更新工作流参数
            // 更新加载图像节点
            workflow = updateWorkflowParams(workflow, "1", "image", uploadedName);

            // 更新关键字提示词
            String keyword = request.getKeyword() != null ? request.getKeyword() : "person";
            workflow = updateWorkflowParams(workflow, "4", "prompt", keyword);

            // 更新阈值
            if (request.getThreshold() != null) {
                workflow = updateWorkflowParams(workflow, "4", "threshold", request.getThreshold());
            }

            // 4. 执行工作流
            String promptId = executeWorkflow(workflow);
            result.setPromptId(promptId);

            // 5. 等待完成
            JsonNode outputs = waitForCompletion(promptId);

            // 6. 下载结果
            String outputFilename = null;
            for (JsonNode nodeOutput : outputs) {
                if (nodeOutput.has("images")) {
                    JsonNode images = nodeOutput.get("images");
                    if (images.isArray() && images.size() > 0) {
                        JsonNode firstImage = images.get(0);
                        String filename = firstImage.get("filename").asText();
                        String subfolder = firstImage.has("subfolder") ?
                                firstImage.get("subfolder").asText() : "";

                        downloadImage(filename, subfolder, "output");
                        outputFilename = filename;
                        break;
                    }
                }
            }

            if (outputFilename != null) {
                result.setSuccess(true);
                result.setOutputFilename(outputFilename);
                result.setOutputUrl("/output/" + outputFilename);
                log.info("关键字抠图成功: {}", outputFilename);
            } else {
                result.setSuccess(false);
                result.setErrorMessage("未找到输出图片");
                log.error("关键字抠图失败: 未找到输出图片");
            }

        } catch (Exception e) {
            log.error("关键字抠图失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        result.setExecutionTime(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 检查 ComfyUI 服务器状态
     */
    public boolean checkServerStatus() {
        try {
            String url = config.getApi().getBaseUrl() + "/system_stats";

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    return response.getCode() == 200;
                }
            }
        } catch (Exception e) {
            log.error("检查服务器状态失败", e);
            return false;
        }
    }
}

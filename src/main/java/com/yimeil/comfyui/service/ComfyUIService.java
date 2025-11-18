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
import org.apache.hc.core5.http.ParseException;
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
    public String uploadImage(MultipartFile file) throws IOException, ParseException {
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
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
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
    public String executeWorkflow(JsonNode workflow) throws IOException, ParseException {
        log.info("提交工作流执行...");

        String url = config.getApi().getBaseUrl() + "/prompt";

        // 构建请求 payload
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("prompt", workflow);
        payload.put("client_id", clientId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(payload), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                log.info("ComfyUI API 响应: {}", responseBody);

                JsonNode jsonNode = objectMapper.readTree(responseBody);

                // 检查是否有错误响应
                if (jsonNode.has("error")) {
                    JsonNode errorNode = jsonNode.get("error");
                    String errorType = errorNode.get("type").asText();
                    String errorMessage = errorNode.get("message").asText();

                    // 提取详细的节点错误信息
                    StringBuilder detailsBuilder = new StringBuilder();
                    if (jsonNode.has("node_errors")) {
                        JsonNode nodeErrors = jsonNode.get("node_errors");
                        nodeErrors.fields().forEachRemaining(entry -> {
                            String nodeId = entry.getKey();
                            JsonNode nodeError = entry.getValue();
                            if (nodeError.has("errors") && nodeError.get("errors").isArray()) {
                                nodeError.get("errors").forEach(err -> {
                                    String details = err.has("details") ? err.get("details").asText() : "";
                                    detailsBuilder.append(String.format("\n节点 %s: %s", nodeId, details));
                                });
                            }
                        });
                    }

                    String fullError = String.format("ComfyUI 错误 [%s]: %s%s",
                            errorType, errorMessage, detailsBuilder.toString());
                    log.error(fullError);
                    throw new IOException(fullError);
                }

                // 检查响应中是否包含 prompt_id
                if (!jsonNode.has("prompt_id")) {
                    log.error("API 响应中没有 prompt_id 字段，完整响应: {}", responseBody);
                    throw new IOException("API 响应格式错误: 缺少 prompt_id 字段");
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
    public JsonNode waitForCompletion(String promptId) throws IOException, InterruptedException, ParseException {
        log.info("等待工作流执行完成，Prompt ID: {}", promptId);

        String url = config.getApi().getBaseUrl() + "/history/" + promptId;

        // 轮询检查执行状态（最多等待 5 分钟）
        int maxAttempts = 60;
        int attempt = 0;

        while (attempt < maxAttempts) {
            Thread.sleep(5000); // 每 5 秒检查一次

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);

                try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                    String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                    JsonNode history = objectMapper.readTree(responseBody);

                    if (history.has(promptId)) {
                        log.info("工作流执行完成");
                        JsonNode promptData = history.get(promptId);
                        log.info("完整的 Prompt 数据: {}", objectMapper.writeValueAsString(promptData));

                        JsonNode outputs = promptData.get("outputs");
                        log.info("输出节点数据: {}", objectMapper.writeValueAsString(outputs));
                        return outputs;
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
    public File downloadImage(String filename, String subfolder, String type, String outputDir) throws IOException {
        log.info("下载图片: filename={}, subfolder={}, type={}, outputDir={}", filename, subfolder, type, outputDir);

        String url = config.getApi().getBaseUrl() + "/view"
                + "?filename=" + filename
                + "&subfolder=" + (subfolder != null ? subfolder : "")
                + "&type=" + (type != null ? type : "output");

        log.info("下载URL: {}", url);

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

            // 3. 更新工作流参数 - 节点 2 是 LoadImage 节点
            workflow = updateWorkflowParams(workflow, "2", "image", uploadedName);

            // 应用自定义参数
            // 节点 10 (SAMDetectorSegmented) 的 threshold 参数
            if (request.getThreshold() != null) {
                workflow = updateWorkflowParams(workflow, "10", "threshold", request.getThreshold());
            }

            // 节点 23 (GrowMaskWithBlur) 的边缘优化参数
            if (request.getAlphaMatting() != null && request.getAlphaMatting()) {
                // 调整边缘模糊参数
                if (request.getAlphaMattingErodeSize() != null) {
                    workflow = updateWorkflowParams(workflow, "23", "expand", -request.getAlphaMattingErodeSize());
                }
            }

            // 4. 执行工作流
            log.debug("runMatting - 更新后的工作流: {}", objectMapper.writeValueAsString(workflow));
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
                        String type = firstImage.has("type") ?
                                firstImage.get("type").asText() : "output";

                        downloadImage(filename, subfolder, type, "output");
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
     * 执行关键字抠图
     *
     * @param imageFile 图片文件
     * @param request 基本请求参数（主要用于工作流名称）
     * @param params 关键字抠图参数
     * @return 抠图结果
     */
    public MattingResult runKeywordMatting(MultipartFile imageFile, MattingRequest request, Map<String, Object> params) {
        MattingResult result = new MattingResult();
        long startTime = System.currentTimeMillis();

        try {
            // 1. 加载关键字抠图工作流
            String workflowName = request.getWorkflowName() != null ?
                    request.getWorkflowName() : "matting_keyword_api.json";
            JsonNode workflow = loadWorkflowFromResource(workflowName);

            // 2. 上传图片
            String uploadedName = uploadImage(imageFile);

            // 3. 更新工作流参数 - 根据 matting_keyword_api.json 的节点结构
            // 节点 1: LoadImage - 加载图片
            workflow = updateWorkflowParams(workflow, "1", "image", uploadedName);

            // 节点 4: ArgosTranslateTextNode - 翻译关键字
            String keyword = (String) params.get("keyword");
            String translateFrom = (String) params.getOrDefault("translateFrom", "chinese");
            log.info("关键字抠图 - 关键字: '{}', 翻译方向: {} -> english", keyword, translateFrom);

            workflow = updateWorkflowParams(workflow, "4", "from_translate", translateFrom);
            workflow = updateWorkflowParams(workflow, "4", "to_translate", "english");
            workflow = updateWorkflowParams(workflow, "4", "text", keyword);

            // 节点 2: LayerMask: SegmentAnythingUltra V2 - SAM + Grounding DINO
            String samModel = (String) params.getOrDefault("samModel", "sam_vit_h (2.56GB)");
            String dinoModel = (String) params.getOrDefault("dinoModel", "GroundingDINO_SwinT_OGC (694MB)");
            Double threshold = (Double) params.getOrDefault("threshold", 0.3);
            log.info("关键字抠图 - SAM参数: model={}, dino={}, threshold={}", samModel, dinoModel, threshold);
            String detailMethod = (String) params.getOrDefault("detailMethod", "VITMatte(local)");
            Integer detailErode = (Integer) params.getOrDefault("detailErode", 6);
            Integer detailDilate = (Integer) params.getOrDefault("detailDilate", 6);
            Double blackPoint = (Double) params.getOrDefault("blackPoint", 0.15);
            Double whitePoint = (Double) params.getOrDefault("whitePoint", 0.99);
            Double maxMegapixels = (Double) params.getOrDefault("maxMegapixels", 2.0);
            String device = (String) params.getOrDefault("device", "cuda");

            workflow = updateWorkflowParams(workflow, "2", "sam_model", samModel);
            workflow = updateWorkflowParams(workflow, "2", "grounding_dino_model", dinoModel);
            workflow = updateWorkflowParams(workflow, "2", "threshold", threshold);
            workflow = updateWorkflowParams(workflow, "2", "detail_method", detailMethod);
            workflow = updateWorkflowParams(workflow, "2", "detail_erode", detailErode);
            workflow = updateWorkflowParams(workflow, "2", "detail_dilate", detailDilate);
            workflow = updateWorkflowParams(workflow, "2", "black_point", blackPoint);
            workflow = updateWorkflowParams(workflow, "2", "white_point", whitePoint);
            workflow = updateWorkflowParams(workflow, "2", "max_megapixels", maxMegapixels);
            workflow = updateWorkflowParams(workflow, "2", "device", device);

            // 4. 执行工作流
            log.info("runKeywordMatting - 准备提交工作流，节点数: {}",
                     workflow.isObject() ? ((ObjectNode)workflow).size() : 0);
            String workflowJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(workflow);
            log.info("runKeywordMatting - 完整工作流JSON:\n{}", workflowJson);
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
                        String type = firstImage.has("type") ?
                                firstImage.get("type").asText() : "output";

                        downloadImage(filename, subfolder, type, "output");
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

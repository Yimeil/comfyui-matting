package com.comfyui.matting.service;

import com.comfyui.matting.model.MattingRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ComfyUI API 服务
 *
 * 负责与 ComfyUI 后端交互
 */
@Service
public class ComfyUIService {

    @Value("${comfyui.server.host:127.0.0.1}")
    private String comfyuiHost;

    @Value("${comfyui.server.port:8188}")
    private String comfyuiPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId = UUID.randomUUID().toString();

    /**
     * 上传图像到 ComfyUI 服务器
     */
    public String uploadImage(MultipartFile file) throws IOException {
        String url = String.format("http://%s:%s/upload/image", comfyuiHost, comfyuiPort);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("image", file.getInputStream(),
                    ContentType.DEFAULT_BINARY, file.getOriginalFilename());
            builder.addTextBody("overwrite", "true");

            httpPost.setEntity(builder.build());

            return httpClient.execute(httpPost, response -> {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                return jsonNode.get("name").asText();
            });
        }
    }

    /**
     * 提交工作流到 ComfyUI
     */
    public String queuePrompt(String imageFilename, String maskFilename, MattingRequest params)
            throws IOException {

        // 加载工作流模板
        JsonNode workflow = loadWorkflowTemplate();

        // 应用参数预设
        applyPreset(workflow, params);

        // 更新输入文件
        ((ObjectNode) workflow.get("2").get("inputs")).put("image", imageFilename);

        // 更新参数
        ((ObjectNode) workflow.get("10").get("inputs"))
                .put("mask_hint_threshold", params.getMaskHintThreshold());
        ((ObjectNode) workflow.get("43").get("inputs"))
                .put("kernel_size", params.getKernelSize());
        ((ObjectNode) workflow.get("23").get("inputs"))
                .put("expand", params.getExpand());
        ((ObjectNode) workflow.get("23").get("inputs"))
                .put("blur_radius", params.getBlurRadius());

        // 提交到 ComfyUI
        String url = String.format("http://%s:%s/prompt", comfyuiHost, comfyuiPort);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);

            Map<String, Object> payload = new HashMap<>();
            payload.put("prompt", workflow);
            payload.put("client_id", clientId);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            httpPost.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

            return httpClient.execute(httpPost, response -> {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                return jsonNode.get("prompt_id").asText();
            });
        }
    }

    /**
     * 获取执行历史
     */
    public JsonNode getHistory(String promptId) throws IOException {
        String url = String.format("http://%s:%s/history/%s", comfyuiHost, comfyuiPort, promptId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            return httpClient.execute(httpGet, response -> {
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                return objectMapper.readTree(responseBody);
            });
        }
    }

    /**
     * 下载结果图像
     */
    public byte[] downloadImage(String filename, String subfolder) throws IOException {
        String url = String.format("http://%s:%s/view?filename=%s&subfolder=%s&type=output",
                comfyuiHost, comfyuiPort, filename, subfolder != null ? subfolder : "");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(url);

            return httpClient.execute(httpGet, response -> {
                HttpEntity entity = response.getEntity();
                return EntityUtils.toByteArray(entity);
            });
        }
    }

    /**
     * 加载工作流模板
     */
    private JsonNode loadWorkflowTemplate() throws IOException {
        // 从 sam_mask_matting_api.json 加载
        Path workflowPath = Path.of("sam_mask_matting_api.json");
        String workflowJson = Files.readString(workflowPath);
        return objectMapper.readTree(workflowJson);
    }

    /**
     * 应用参数预设
     */
    private void applyPreset(JsonNode workflow, MattingRequest params) {
        switch (params.getPreset()) {
            case "portrait": // 人像抠图
                params.setMaskHintThreshold(0.7);
                params.setExpand(-4);
                params.setBlurRadius(2.5);
                break;
            case "product": // 产品图
                params.setKernelSize(10);
                params.setExpand(-1);
                params.setBlurRadius(0.3);
                break;
            case "hair": // 毛发细节
                params.setExpand(-1);
                params.setBlurRadius(1.5);
                break;
            default: // 默认参数
                break;
        }
    }

    /**
     * 检查 ComfyUI 服务器状态
     */
    public boolean checkServerStatus() {
        try {
            String url = String.format("http://%s:%s/system_stats", comfyuiHost, comfyuiPort);
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet httpGet = new HttpGet(url);
                return httpClient.execute(httpGet, response -> response.getCode() == 200);
            }
        } catch (Exception e) {
            return false;
        }
    }
}

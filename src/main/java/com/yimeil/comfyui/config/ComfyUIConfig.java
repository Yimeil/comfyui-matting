package com.yimeil.comfyui.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ComfyUI 配置
 * 从 application.yml 中读取配置
 */
@Configuration
@ConfigurationProperties(prefix = "comfyui")
@Data
public class ComfyUIConfig {

    /**
     * API 配置
     */
    private ApiConfig api = new ApiConfig();

    /**
     * 工作流配置
     */
    private WorkflowConfig workflow = new WorkflowConfig();

    @Data
    public static class ApiConfig {
        /**
         * ComfyUI API 基础 URL
         */
        private String baseUrl = "http://127.0.0.1:8188";

        /**
         * 连接超时（毫秒）
         */
        private int connectTimeout = 10000;

        /**
         * 读取超时（毫秒）
         */
        private int readTimeout = 300000;
    }

    @Data
    public static class WorkflowConfig {
        /**
         * 工作流文件目录
         */
        private String directory = "workflows";

        /**
         * 默认工作流文件名
         */
        private String defaultWorkflow = "sam_matting.json";
    }
}

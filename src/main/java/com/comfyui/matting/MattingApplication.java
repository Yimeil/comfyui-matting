package com.comfyui.matting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ComfyUI 图像抠图 Web 应用主类
 *
 * 轻量级解决方案，使用 Java + HTML 替代 Gradio
 */
@SpringBootApplication
public class MattingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MattingApplication.class, args);
        System.out.println("===========================================");
        System.out.println("图像抠图 Web 应用已启动！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("===========================================");
    }
}

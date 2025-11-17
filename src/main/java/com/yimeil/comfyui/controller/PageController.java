package com.yimeil.comfyui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面控制器
 *
 * Vue 前端位于 src/main/resources/static/index.html
 * Spring Boot 会自动从 static 目录提供静态资源
 */
@Controller
public class PageController {

    /**
     * 首页 - 重定向到 Vue 应用
     *
     * 由于我们使用 static/index.html，Spring Boot 会自动处理 "/"
     * 这个方法可以保留用于未来扩展，或者添加服务端渲染逻辑
     */
    @GetMapping("/")
    public String index() {
        // Spring Boot 会自动从 static 目录提供 index.html
        return "forward:/index.html";
    }
}

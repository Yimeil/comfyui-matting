package com.yimeil.comfyui;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * ComfyUI Matting Service 启动类
 *
 * @author Yimeil
 */
@SpringBootApplication
@Slf4j
public class ComfyuiMattingApplication {

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext application = SpringApplication.run(ComfyuiMattingApplication.class, args);

        Environment env = application.getEnvironment();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path", "/");

        log.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! Access URLs:\n\t" +
                "Local: \t\thttp://localhost:{}{}\n\t" +
                "External: \thttp://{}:{}{}\n\t" +
                "Swagger: \thttp://{}:{}{}/swagger-ui.html\n" +
                "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                port,
                contextPath,
                ip,
                port,
                contextPath,
                ip,
                port,
                contextPath
        );

        log.info("\n" +
                "================================================================================\n" +
                "   ____            __       _   _ ___   __  __       _   _   _             \n" +
                "  / ___|___  _ __ / _|_   _| | | |_ _| |  \\/  | __ _| |_| |_(_)_ __   __ _ \n" +
                " | |   / _ \\| '_ \\| |_| | | | | | || |  | |\\/| |/ _` | __| __| | '_ \\ / _` |\n" +
                " | |__| (_) | | | |  _| |_| | |_| || |  | |  | | (_| | |_| |_| | | | | (_| |\n" +
                "  \\____\\___/|_| |_|_|  \\__, |\\___/|___| |_|  |_|\\__,_|\\__|\\__|_|_| |_|\\__, |\n" +
                "                       |___/                                          |___/ \n" +
                "================================================================================\n" +
                ":: ComfyUI Matting Service :: (v1.0.0)\n" +
                ":: 简单易用的 AI 智能抠图服务 ::\n" +
                "================================================================================\n"
        );
    }
}

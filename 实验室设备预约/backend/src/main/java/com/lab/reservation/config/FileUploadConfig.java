package com.lab.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 文件上传配置类
 * 读取配置文件中的文件上传路径
 */
@Configuration
public class FileUploadConfig {

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${file.allowed-extensions}")
    private String allowedExtensions;

    public String getUploadPath() {
        return uploadPath;
    }

    public String[] getAllowedExtensions() {
        return allowedExtensions.split(",");
    }
}

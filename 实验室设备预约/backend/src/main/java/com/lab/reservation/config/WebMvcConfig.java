package com.lab.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.io.File;
import java.util.List;

/**
 * Web MVC配置类
 * 配置JSON序列化和时间格式化
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final OperationLogInterceptor operationLogInterceptor;

    @Value("${file.upload-path}")
    private String uploadPath;

    public WebMvcConfig(OperationLogInterceptor operationLogInterceptor) {
        this.operationLogInterceptor = operationLogInterceptor;
    }

    /**
     * 映射本地上传目录，供前端通过 /api/uploads/** 访问设备图片等静态文件
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String loc = uploadPath == null ? "" : uploadPath.trim().replace("\\", "/");
        
        // 处理相对路径，转换为绝对路径
        if (!loc.isEmpty() && !new File(loc).isAbsolute()) {
            // 获取项目根目录
            String projectRoot = System.getProperty("user.dir");
            loc = projectRoot + File.separator + loc;
        }
        
        if (!loc.isEmpty() && !loc.endsWith(File.separator)) {
            loc = loc + File.separator;
        }
        
        if (!loc.isEmpty()) {
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations("file:" + loc.replace("\\", "/"));
        }
    }

    /**
     * 配置JSON消息转换器，统一处理时间格式化
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        // 注册Java8时间模块
        mapper.registerModule(new JavaTimeModule());
        // 禁用日期时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(mapper);
        converters.add(0, converter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(operationLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/sys/log/**");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new StringToNullArgumentResolver());
    }
}
